package com.example.mall.service.impl;

import com.example.mall.entity.OrderItem;
import com.example.mall.entity.Product;
import com.example.mall.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.example.mall.service.ProductService;
import com.example.mall.service.ShopCartService;

import javax.naming.InsufficientResourcesException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class ShopCartServiceImpl implements ShopCartService {

    @Autowired
    private ProductService productService;

    //用redis来操作购物车
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 加购物车
     * 将商品id保存到redis中List中
     */
    @Override
    public void addCart(int productId, HttpServletRequest request) throws Exception {
        User loginUser = (User) request.getSession().getAttribute("user");
        if (loginUser == null) {
            throw new Exception("未登录！请重新登录");
        }
        redisTemplate.opsForList().rightPush(NAME_PREFIX + loginUser.getId(), productId);
//        List<Integer> productIds = (List<Integer>) request.getSession().getAttribute(NAME_PREFIX + loginUser.getId());
//        if (productIds == null) {
//            productIds = new ArrayList<>();
//            request.getSession().setAttribute(NAME_PREFIX + loginUser.getId(), productIds);
//        }
//        productIds.add(productId);
    }

    /**
     * 移除
     *
     * 移除redis List中对应的商品Id
     */
    @Override
    public void remove(int productId, HttpServletRequest request) throws Exception {
        User loginUser = (User) request.getSession().getAttribute("user");
        if (loginUser == null) {
            throw new Exception("未登录！请重新登录");
        }
        List<Integer> productIds = redisTemplate.opsForList().range(NAME_PREFIX + loginUser.getId(), 0, -1);
        Iterator<Integer> iterator = productIds.iterator();
        while (iterator.hasNext()) {
            if (productId == iterator.next()) {
                iterator.remove();
                redisTemplate.opsForList().remove(NAME_PREFIX + loginUser.getId(), 1, productId);
            }
        }
    }

    /**
     * 查看购物车
     *
     * 查询出redis的List中所有的商品Id,并封装成List<OrderItem>返回
     */
    @Override
    public List<OrderItem> listCart(HttpServletRequest request) throws Exception {
        User loginUser = (User) request.getSession().getAttribute("user");
        if (loginUser == null) {
            throw new Exception("未登录！请重新登录");
        }
        List<Integer> productIds = redisTemplate.opsForList().range(NAME_PREFIX + loginUser.getId(), 0, -1);
        // key: productId value:OrderItem
        Map<Integer, OrderItem> productMap = new HashMap<>();
        if (productIds == null){
            return new ArrayList<>();
        }
        // 遍历List中的商品id，每个商品Id对应一个OrderItem
        for (Integer productId : productIds) {
            if (productMap.get(productId) == null) {
                Product product = productService.findById(productId);
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setProductId(productId);
                orderItem.setCount(1);
                orderItem.setSubTotal(product.getShopPrice());
                productMap.put(productId, orderItem);
            } else {
                OrderItem orderItem = productMap.get(productId);
                int count = orderItem.getCount();
                orderItem.setCount(++count);
                Double subTotal = orderItem.getSubTotal();
                orderItem.setSubTotal(orderItem.getSubTotal()+subTotal);
                productMap.put(productId, orderItem);
            }
        }
        List<OrderItem> orderItems = new ArrayList<>(productMap.values());
        return orderItems;
    }

    /**
     *提交订单时清空购物车
     */
    @Override
    public void clear(HttpServletRequest request) throws Exception {
        User loginUser = (User) request.getSession().getAttribute("user");
        if (loginUser == null) {
            throw new Exception("未登录！请重新登录");
        }
        redisTemplate.delete(NAME_PREFIX + loginUser.getId());
    }
}
