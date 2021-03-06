package com.zhongyp.redis;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhongyp.
 * @date 2019/7/1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-data-redis.xml"})
public class TestRedis {

    @Autowired
    @Qualifier("testRedisTemplate")
    RedisTemplate redisTemplate;

    @Autowired
    RedisUtils redisUtils;


    @Test
    public void testSetAndGet(){
        redisTemplate.opsForValue().set("abctest","testabc");
        System.out.println(redisTemplate.opsForValue().get("abctest"));
    }

    /**
     * 测试序列化后的对象是否可以强制类型转换
     *
     * 结论：如果存入的value是个对象，则redis中存储的是对象的序列化
     */
    @Test
    public void testHash(){
        HashMap map = new HashMap<>();
        List list = new ArrayList<>();
        list.add("aa");
        map.put("java/lang/a","b");
        map.put("c","b");
        map.put("b","b");
        map.put("d","b");
        map.put("list", list);
        redisTemplate.opsForHash().put("hashkey", "test", map);
        Map map1 = redisTemplate.opsForHash().entries("hashkey");
        Map map2 = (Map) map1.get("test");
        System.out.println(map2.get("list"));
        System.out.println(redisTemplate.opsForHash().entries("hashkey"));

    }

    /**
     * 测试redis pipline
     * pipline是管道的意思
     * pipline可以支持一次执行多个命令，只获取一次connection，用于执行批量任务时，提高系统性能
     */
    @Test
    public void testPiplineWithRedisConnection(){
        long start = System.currentTimeMillis();
        for(int i=0;i<10;i++) {
            redisTemplate.executePipelined(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                    redisConnection.listCommands().lPush("123a".getBytes(), "abc".getBytes());
                    redisConnection.listCommands().lPush("1234b".getBytes(), "abc3".getBytes());
                    redisConnection.listCommands().lPush("1235c".getBytes(), "abc4".getBytes());
                    redisConnection.listCommands().lPush("1236d".getBytes(), "abc5".getBytes());
                    redisConnection.listCommands().lPush("1236e".getBytes(), "abc5".getBytes());
                    return null;
                }
            });
        }
        System.out.println(System.currentTimeMillis()-start);

    }

    /**
     * 测试RedisTemplate execute和executePipelined对事务管理器的支持
     */
    @Test
    public void testRedisTransaction(){
        long start = System.currentTimeMillis();
//        for(int i=0;i<100000;i++){
            // execute()方法事务支持
            redisUtils.testExecuteTransactionWithRedisCallback();
            // executePipelined方法事务支持
//            redisUtils.testExecutePipelineTransactionWithRedisCallback();
            // executePipelined方法事务支持
//            redisUtils.testExecutePipelineWithConnection();
//            System.out.println(i);
//        }
        System.out.println(System.currentTimeMillis()-start);
    }

    /**
     * 测试RedisTemplate 游标
     */
    @Test
    public void testRedisTemplateScan(){

        redisTemplate.opsForSet().add(2, "daaa");
        Cursor cursor1 = redisTemplate.opsForSet().scan(2, new ScanOptions.ScanOptionsBuilder().match("*d*").count(1000).build());

        while (cursor1.hasNext()){
            System.out.println(cursor1.next());
        }
    }

    /**
     * 测试key不存在和value为null的返回值是否相同
     */
    @Test
    public void testNullValue(){
        redisTemplate.opsForValue().set("hello", null);
        redisTemplate.opsForHash().put("java/lang/a", "b", "null");
        redisTemplate.delete("java/lang/a");
        System.out.println(isNull((String) redisTemplate.opsForValue().get("hello")));
        System.out.println(isNull((String) redisTemplate.opsForValue().get("java/lang/a")));

    }

    public static boolean isNull(String str) {
        return str == null;
    }

    /**
     * 测试key绑定operations
     */
    @Test
    public void testBoundValueOps(){
//        Role role = new Role();
//        role.setId(10);
//        role.setRoleName("CEO");
//        SessionCallback sessionCallback = new SessionCallback<Role>() {
//            @Override
//            public Role execute(RedisOperations redisOperations) throws DataAccessException {
//                redisOperations.boundValueOps("role_1").set(role);
//                return (Role) redisOperations.boundValueOps("role_1").get();
//            }
//        };
//        Role role1 = (Role) redisTemplate.execute(sessionCallback);
//        System.out.println(role1.getRoleName());

         SessionCallback sessionCallback = new SessionCallback() {
             @Override
             public Object execute(RedisOperations redisOperations) throws DataAccessException {
                 BoundValueOperations operations = redisOperations.boundValueOps("abc");
                 operations.set("abcd");
                 operations.append("ok");
                 operations.getAndSet("java/lang/a");
                 operations.append("vc");
                 return operations.get();
             }
         };
        Object object = redisTemplate.executePipelined(sessionCallback);
        System.out.println(object);
        System.out.println(((List) object).get(2));
        Map<String, String> map = new HashMap<>();

    }

    /**
     * 测试json序列化方式对list和map的转换
     */
    @Test
    public void testJson(){
        Map map = new HashMap();
        map.put("java/lang/a", "b");
        List list = new ArrayList();
        list.add(map);
        redisTemplate.opsForHash().put("java/lang/a", "b", list);
        Object object = redisTemplate.opsForHash().get("java/lang/a", "b");
        /**
         * [
         * 	"java.util.ArrayList",
         * 	[
         *                {
         * 			"@class": "java.util.HashMap",
         * 			"java.lang.a": "b"
         *        }
         * 	]
         * ]
         */
        //虽然输出是[{java.lang.a=b}]，但是存储的是如上述注释的样子
        System.out.println(object);
    }

    @Test
    public void hashKey(){
        System.out.println(redisTemplate.hasKey("*8201*"));
    }

    /**
     * 测试pipline是否可以单条返回值
     *
     * 测试结果：
     *
     * execute可以有单条返回值
     * executePipelined返回值为null
     *
     */
    @Test
    public void testPiplinedData(){


        List list = new ArrayList();
        // 先将100000key插入进去
       redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> redisOperations) throws DataAccessException {
                redisOperations.multi();
                ValueOperations valueOperations = redisOperations.opsForValue();
                for(int i=0;i<100000;i++){
                    valueOperations.set("java/lang/a" + i, i);
                }
                redisOperations.exec();
                return null;
            }
        });
       // 使用execute和executePipelined分别测试，executePipeline获取值为null
        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> redisOperations) throws DataAccessException {
                ValueOperations valueOperations = redisOperations.opsForValue();
                for(int i=0;i<100000;i++){
                    list.add(valueOperations.get("java/lang/a" + i));
                }
                return null;
            }
        });
        for(Object obj:list){
            System.out.println(obj);
        }


    }


    /**
     * 测试pipline是否可以单条返回值
     */
    @Test
    public void testExecute(){


        List list = new ArrayList();
//        List list1 = redisTemplate.executePipelined(new SessionCallback<Object>() {
//            @Override
//            public <K, V> Object execute(RedisOperations<K, V> redisOperations) throws DataAccessException {
//                redisOperations.multi();
//                ValueOperations valueOperations = redisOperations.opsForValue();
//                valueOperations.set("1", "2");
//                valueOperations.set("2", "2");
//                valueOperations.set("3", "2");
//                valueOperations.get("87*");
//                for(int i=1;i<4;i++){
//                    list.add(valueOperations.get(String.valueOf(i)));
//                }
//                redisOperations.exec();
//                return null;
//            }
//        });


//        for(Object object:list1){
//            System.out.println(object);
//        }

        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                redisOperations.multi();
                ValueOperations valueOperations = redisOperations.opsForValue();
                valueOperations.set("1", "2");
                valueOperations.set("2", "2");
                valueOperations.set("3", "2");
                int b = 1/0;
                valueOperations.get("87*");
                for(int i=1;i<4;i++){
                    list.add(valueOperations.get(String.valueOf(i)));
                }
                redisOperations.exec();
                return null;
            }

        });
    }

    /**
     * 测试模糊查询，测试连接到底需要需要手动关闭，结果还是需要的手动关闭的
     */
    @Test
    public void testFuzzyQuery(){

//        SetOperations setOperations = redisTemplate.opsForSet();
//        Cursor cursor = setOperations.scan("2019:87:pay:allkeyset", ScanOptions.scanOptions().count(100).match("*2019:8*:*").build());
//        int i=0;
//        while (cursor.hasNext()){
//            System.out.println(i++);
//            System.out.println(cursor.next());
//        }


        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
        Cursor redisCursor = connection.scan(ScanOptions.scanOptions().count(100).match("*2019:8*:*").build());
        int i=0;
        while (redisCursor.hasNext()){
            System.out.println(i++);
            System.out.println(redisCursor.next());
        }

    }

    /**
     * 用来测试Redis的valueOperations存储实例
     */
    @Test
    public void testSaveObject(){
        Role role = new Role();
        role.setId(1);
        role.setRoleName("abc");
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("role", role);
        System.out.println(valueOperations.get("role"));
    }

    /**
     * 用来测试不同序列化方式的valueOperations放入Object对象时的不同处理方式
     */
    @Test
    public void testObject(){
        HashMap hashMap = new HashMap();
        hashMap.put("test", "test");

        redisTemplate.executePipelined(new SessionCallback<Object>() {

            @Override
            public <K, V> Object execute(RedisOperations<K, V> redisOperations) throws DataAccessException {
                ValueOperations operations = redisOperations.opsForValue();
                operations.set("test", hashMap);
                return null;
            }
        });
    }

    @Test
    public void testStoreChinese(){
        HashOperations hashOperations = redisTemplate.opsForHash();
        hashOperations.put("abc", "java/lang/a", "我是中国人!");
        System.out.println(hashOperations.get("abc", "java/lang/a"));
    }


    @Test
    public void testExpireKeys(){
//        redisTemplate.executePipelined(new SessionCallback<Object>() {
//            @Override
//            public <K, V> Object execute(RedisOperations<K, V> redisOperations) throws DataAccessException {
//
//                for(int i=0; i<10000;i++){
//                    ValueOperations valueOperations = redisOperations.opsForValue();
//                    valueOperations.set("java/lang/a" + i, i, 10);
//                    redisTemplate.expire("java/lang/a" + i, 10, TimeUnit.SECONDS);
//                }
//                return null;
//            }
//        });
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        String key = "abcdefg";
        String value = "value-123456";
        redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                redisConnection.set(key.getBytes(), value.getBytes());

                redisConnection.expire(key.getBytes(), 3);
                return null;
            }
        });


    }

    @Test
    public void testPipelinedException(){
        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> redisOperations) throws DataAccessException {
                redisOperations.multi();
                ValueOperations valueOperations = redisOperations.opsForValue();
                valueOperations.set("abcd", "abcd");
//                int i = 1/0;
                redisOperations.exec();
                return null;
            }
        });
        System.out.println(redisTemplate.delete("abc"));

    }

}

class Role implements Serializable {
    int id;

    String roleName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
