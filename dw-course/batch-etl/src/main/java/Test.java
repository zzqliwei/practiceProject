public class Test {
    public static void main(String[] args) {
        String str = "MLDN中中心Java技术学习班20130214开班啦";//定义字符串
        //将字符串进行拆分，找到其中的日期数据
        String temp[] = str.split("\\D+",0);
        System.out.println(temp[1]);
    }
}
