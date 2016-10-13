package com.lh.jni;

/**
 * Created by LuHao on 2016/10/10.
 */
public class JniKit {

    static {
        System.loadLibrary("JniDemo");
    }

    /**如果你的native方法报错，没关系，配置完成自然会编译通过
     * @param num
     * @return
     */
    public static native int calculate(int num);

}
