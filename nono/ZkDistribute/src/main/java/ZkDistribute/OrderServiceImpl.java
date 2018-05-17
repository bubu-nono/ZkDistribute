package ZkDistribute;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 订单实现
 * Created by nono on 2018/5/16.
 */
public class OrderServiceImpl implements OrderServer {

    private static OrderCodeGenerator ocg =  new OrderCodeGenerator();
    private static ZkDistributeImproveLock lock = new ZkDistributeImproveLock("/locks");

    @Override
    public void createOrder() {
        String orderCode = null;
        try {
            // 加锁
            lock.lock(5000, TimeUnit.MILLISECONDS);
//            lock.lock();
            orderCode = ocg.getOrderCode();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
             // 释放锁
            lock.unlock();
        }
        System.out.println(Thread.currentThread().getName()+" 创建订单号："+ orderCode);
    }
}
