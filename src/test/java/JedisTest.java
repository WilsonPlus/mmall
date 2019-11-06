import com.mmall.util.RedisPoolUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * @author YaningLiu
 * @date 2018/9/23/ 15:04
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:applicationContext.xml"})
public class JedisTest {
    @Autowired
    private ShardedJedisPool shardedJedisPool;
    @Autowired
    private JedisPool jedisPool;
    @Autowired
    private RedisPoolUtil redisPoolUtil;

    @Test
    public void accRedisTest() {
/*        ApplicationContext ac = new ClassPathXmlApplicationContext("applicationContext-redis.xml");
        JedisPool jedisPool = (JedisPool) ac.getBean("jedisPool");*/
        Jedis jedis = jedisPool.getResource();
        jedis.set("keyyyyyyyyyyyyyy", "valueeeeeeeeeeeeee");
        jedisPool.returnResource(jedis);
    }

    @Test
    public void redisPoolUtilTest() {
        /*RedisPoolUtil.set("newkey","new value");
        String  key = RedisPoolUtil.get("newkey");
        System.out.println(key);*/

        redisPoolUtil.set("keyTest", "value");

        String value = redisPoolUtil.get("keyTest");

        redisPoolUtil.setEx("keyex", "valueex", 60 * 10);

        redisPoolUtil.expire("keyTest", 60 * 20);

        redisPoolUtil.del("keyTest");


        String v = redisPoolUtil.get(null);
        System.out.println(v);

        System.out.println("end");
    }

    @Test
    public void redisShardedPool() {
        ShardedJedis jedis = shardedJedisPool.getResource();
        RedisShardedPoolUtil.set("keyTest","value");
        String value = RedisShardedPoolUtil.get("keyTest");
        RedisShardedPoolUtil.setEx("keyex","valueex",60*10);
        RedisShardedPoolUtil.expire("keyTest",60*20);
        RedisShardedPoolUtil.del("keyTest");

        String aaa = RedisShardedPoolUtil.get(null);
        System.out.println(aaa);
        System.out.println("end");
    }
}
