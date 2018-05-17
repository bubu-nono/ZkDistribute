package ZkDistribute;

import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

import java.io.UnsupportedEncodingException;

/**
 * 序列化，反序列化工具，防止数据传输报错
 * Created by nono on 2018/5/16.
 */
public class MyZkSerializer implements ZkSerializer{
    String charset = "UTF-8";
    @Override
    public byte[] serialize(Object o) throws ZkMarshallingError {
        try {
            return String.valueOf(o).getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new ZkMarshallingError(e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws ZkMarshallingError {
        try {
            return new String(bytes,charset);
        } catch (UnsupportedEncodingException e) {
            throw new ZkMarshallingError(e);
        }
    }
}
