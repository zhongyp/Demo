package com.zhongyp.advanced.refect;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/6/20.
 */
public class ReflectDemo {

    public static void main(String arsg[]) throws Exception{
        //加载Class对象
        User user = new User();
        Class<?> clazz =  Class.forName("com.zhongyp.advanced.refect.User");
        Class<?> clazz1 =  Class.forName("com.zhongyp.advanced.refect.User");
        Map map = new HashMap<>();
        map.put(null, null);
        System.out.println(map.get(null));
        //        Method mtd = clazz.getMethod("setName", String.class);
//        Object reflectDemo = new ReflectDemo();
//
//        //实例化Class对象
//        Object obj = (Object)clazz.newInstance();
//        mtd.invoke(obj,"camda");
    }

}
