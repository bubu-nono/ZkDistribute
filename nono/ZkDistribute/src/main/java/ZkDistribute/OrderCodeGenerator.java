package ZkDistribute;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 创建订单号
 * Created by nono on 2018/5/16.
 */
public class OrderCodeGenerator {

    // 自增长序列
    private static int i = 0;

    public String getOrderCode () {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-");
        return sdf.format(now)+ ++i;
    }
}
