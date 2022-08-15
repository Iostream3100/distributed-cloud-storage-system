package com.example.centralServer.redisLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("distributedLock/")
public class LockController {
    //锁标识key的前缀，后面加上自己的需加锁业务的资源标识码。
    private final String lockPreKey = "DistributedLockKey";

    @Autowired
    private RedisLockTool redisLock;

    /**
     * 测试加锁
     *
     * @param id       加锁的资源id
     * @param identity 身份标识
     * @return
     */
    @GetMapping("lock")
    public String lock(@RequestParam("id") String id,
                       @RequestParam("identity") String identity) {
        String lockKey = lockPreKey + ":" + id;
        boolean lockSuccess = redisLock.getLock(lockKey, identity, 60);
        String result = "lock failed";
        if (lockSuccess) {
            result = "lock success";
        }
        return result;
    }

    /**
     * 测试释放锁
     *
     * @param id       释放锁的资源id
     * @param identity 身份标识
     * @return
     */
    @GetMapping("unlock")
    public String release(@RequestParam("id") String id,
                          @RequestParam("identity") String identity) {
        String lockKey = lockPreKey + ":" + id;
        boolean releaseSuccess = (boolean) redisLock.unlock(lockKey, identity);
        String result = "release failed";
        if (releaseSuccess) {
            result = "release success";
        }
        return result;
    }
}