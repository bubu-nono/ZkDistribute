package ZkDistribute;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 根据zookeeper的同父子节点唯一的特性
 * 使用临时顺序节点实现分布式锁
 * Created by nono on 2018/5/16.
 */
public class ZkDistributeImproveLock implements Lock {
    /**
     * ZK客户端
     */
    private ZkClient client;

    /**
     * 父节点
     */
    private String parentPath;

    /**
     * 当前创建的节点
     */
    private ThreadLocal<String> currentPath = new ThreadLocal<>();

    /**
     * 前一个节点
     */
    private ThreadLocal<String> beforePath = new ThreadLocal<>();

    public ZkDistributeImproveLock(String parentPath) {
        super();
        this.parentPath = parentPath;
        client = new ZkClient("127.0.0.1:2181");
        client.setZkSerializer(new MyZkSerializer());
        // 如果不存在，创建父节点
        if (!this.client.exists(parentPath)) {
            this.client.createPersistent(parentPath);
        }
    }

    /**
     * 尝试获得锁
     * 同父子节点不可重复
     * 加锁的过程，尝试去创建子节点
     * 如果当前创建的子节点是最小的，则获得锁
     * 如果不是，则获取前一个子节点，注册监听用
     * @return
     */
    @Override
    public boolean tryLock() throws ZkNodeExistsException {

        // 如果当前节点不为空，则创建当前子节点
        if (this.currentPath.get() == null) {
            // 创建临时顺序节点
            this.currentPath.set(this.client.createEphemeralSequential(parentPath+"/",""));
        }

        // 获得所有子节点
        List<String> children = this.client.getChildren(parentPath);
        // 排序
        Collections.sort(children);
        // 判断当前节点是否是最小的
        if (this.currentPath.get().equals(parentPath.concat("/").concat(children.get(0)))) {
            return true;
        } else {
            // 取得当前节点的下标
            int currIndex = children.indexOf(this.currentPath.get().substring(parentPath.length() + 1));
            // 取得前一个节点的名字
            this.beforePath.set(this.parentPath.concat("/").concat(children.get(currIndex - 1)));
        }
        return false;
    }

    /**
     * 获得锁
     */
    @Override
    public void lock() {
        //尝试获得锁
        if (!tryLock()) {
            // 不能获得锁，就需要注册监听，阻塞自己，在监听里面要唤醒自己，再次尝试获得锁
            waitForLock(-1,null);
            // 唤醒后，尝试获得锁
            lock();
        }
    }

    /**
     * 获得锁
     * @param time 等待时间
     * @param unit 时间单位
     * @throws Exception
     */
    public void lock(long time, TimeUnit unit) throws Exception {

        if (!tryLock(time, unit)) {
            // 不能获得锁，就需要注册监听，阻塞自己，在监听里面要唤醒自己，再次尝试获得锁
            waitForLock(time, unit);
            // 唤醒后，尝试获得锁
            lock(time, unit);
        }
    }

    /**
     * 等待再次尝试获得锁
     * @param time 等待时间
     * @param unit 时间单位
     */
    private void waitForLock(long time, TimeUnit unit) {
        // 计数器，用来阻塞自己
        final CountDownLatch cd = new CountDownLatch(1);
        // 创建监听
        IZkDataListener listener = new IZkDataListener() {
            // 监听节点数据被修改
            @Override
            public void handleDataChange(String s, Object o) throws Exception {
            }

            // 监听节点被删除
            @Override
            public void handleDataDeleted(String s) throws Exception {
                // 节点被删除，唤醒自己
                cd.countDown();
                System.out.println("-------监听到节点被删除");
            }
        };
        // 注册前一个子节点的监听（避免惊群效应）
        client.subscribeDataChanges(this.beforePath.get(), listener);

        // 再次判断节点是否存在，防止子节点不存在还进行阻塞（避免死锁）
        if (this.client.exists(this.beforePath.get())) {
            // 节点存在
            try {
                // 阻塞自己
                if (time > 0 && unit != null) {
                    cd.await(time, unit);
                } else {
                    cd.await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 醒来后，取消前一个节点的监听注册
        this.client.unsubscribeDataChanges(this.beforePath.get(),listener);
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        // 释放锁，删除节点
        this.client.delete(this.currentPath.get());
    }

    /**
     * 尝试获得锁
     * @param time 等待时间
     * @param unit 时间单位
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        // 如果当前节点不为空，则创建当前子节点
        if (this.currentPath.get() == null) {
            // 创建临时顺序节点
            this.currentPath.set(this.client.createEphemeralSequential(parentPath+"/",""));
        }

        // 获得所有子节点
        List<String> children = this.client.getChildren(parentPath);
        // 排序
        Collections.sort(children);
        // 判断当前节点是否是最小的
        if (this.currentPath.get().equals(parentPath.concat("/").concat(children.get(0)))) {
            return true;
        } else {
            // 取得当前节点的下标
            int currIndex = children.indexOf(this.currentPath.get().substring(parentPath.length() + 1));
            // 取得前一个节点的名字
            this.beforePath.set(this.parentPath.concat("/").concat(children.get(currIndex - 1)));
        }
        return false;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
