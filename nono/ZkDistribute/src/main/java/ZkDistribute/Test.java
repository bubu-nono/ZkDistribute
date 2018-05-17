package ZkDistribute;

import java.util.concurrent.CountDownLatch;

/**
 * 模拟高并发下，订单号的创建（保证订单号的唯一性）
 * Created by nono on 2018/5/16.
 */
public class Test {

    public static void main(String[] args) {
        // 模拟高并发环境
        int currs = 100;
        CountDownLatch cd = new CountDownLatch(currs);

        for (int i = 0;i < currs;i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    OrderServer os = new OrderServiceImpl();
                    os.createOrder();
                }
            }).start();
            cd.countDown();
        }
    }
}
