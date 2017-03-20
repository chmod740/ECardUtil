package me.hupeng.java.ecardutil;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by HUPENG on 2017/3/1.
 */
public class EcardUtil {


    /**
     * 管理员用户的用户名与密码
     * */
    private static final String username = "guest";
    private static final String password = "123456";

    /**
     * 学号
     * */
    private String account;



    /**
     * 查询的结果集合
     * */
    private List<EcardUtil.AccountBill>accountBills = new LinkedList<>();

    public EcardUtil(String account ){
        this.account = account;
        login();
        return;
    }

    public List<AccountBill> getResult(){
        return this.accountBills;
    }

    /**
     * OkHttp库，主要用来执行网络请求
     * */
    private OkHttpClient client = new OkHttpClient.Builder().cookieJar(new CookieJar() {
        private final HashMap<String, List<Cookie>> cookieStore = new HashMap<String, List<Cookie>>();
        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            cookieStore.put(url.host(), cookies);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = cookieStore.get(url.host());
            return cookies != null ? cookies : new ArrayList<Cookie>();
        }
    }).connectTimeout(600, TimeUnit.SECONDS).build();



    /**
     * 执行一次登录操作获取有效的Cookie信息
     * */
    private void login(){
        /**
         * 构造一个POST form表单
         * */
        //name=guest&passwd=123456&rand=2742&imageField.x=0&imageField.y=0&userType=4
        RequestBody formBody = new FormBody.Builder()
                .add("name", username)
                .add("passwd", password)
                .add("rand", "2742")
                .add("imageField.x", "0")
                .add("imageField.y", "0")
                .add("userType", "4")
                .build();
        final Request request = new Request.Builder()
                .url("http://ecard.imu.edu.cn/loginManager.action")
                .post(formBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            getContinueIdToInputAccount();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                accountBillListener.done(null,e);
//                return;
//            }
//
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                getContinueIdToInputAccount();
//                return;
//            }
//        });
    }

    private void getContinueIdToInputAccount()  {
        /**
         * 用 Request.Builder 构造一个request对象
         * */
        Request.Builder requestBuilder = new Request.Builder().url("http://ecard.imu.edu.cn/managerTrjn.action");
        requestBuilder.method("GET", null);
        Request request = requestBuilder.build();

        /**
         * 执行请求操作
         * */
        try {
            Response response= client.newCall(request).execute();
            inputAccount(getContinueKey(response.body().string()));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        mCall.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                accountBillListener.done(null,e);
//                return;
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                inputAccount(getContinueKey(response.body().string()));
//                return;
//            }
//        });
    }


    /**
     *
     * 输入账户名称
     * */
    private void inputAccount(String continueKey){
        //type=2&account=0141120997
        RequestBody formBody = new FormBody.Builder()
                .add("type", "2")
                .add("account", this.account)
                .build();
        final Request request = new Request.Builder()
                .url("http://ecard.imu.edu.cn/managerTrjn.action?" + continueKey)
                .post(formBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            goToJumpPage(getContinueKey(response.body().string()));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                accountBillListener.done(null,e);
//                return;
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                goToJumpPage(getContinueKey(response.body().string()));
//                return;
//            }
//        });
    }

    /**
     * 选择查询内容等参数，进入跳转等待页面
     * */
    private void goToJumpPage(String continueKey){

        RequestBody formBody = new FormBody.Builder()
                .add("searchObject", "15")
                .add("tableType", "3")
                .build();
        final Request request = new Request.Builder()
                .url("http://ecard.imu.edu.cn/managerTrjn.action?" + continueKey)
                .post(formBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            getResult(getContinueKey(response.body().string()));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                accountBillListener.done(null,e);
//                return;
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                getResult(getContinueKey(response.body().string()));
//                return;
//            }
//        });
    }

    /**
     * 跳转到最终结果页面
     * */
    private void getResult(String continueKey){
        RequestBody formBody = new FormBody.Builder()
                .build();
        final Request request = new Request.Builder()
                .url("http://ecard.imu.edu.cn/managerTrjn.action?" + continueKey)
                .post(formBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            boolean continueFlag = true;
            Document document= Jsoup.parse(response.body().string());
            if (accountBills == null){
                accountBills = new LinkedList<>();
            }

            Elements tr=document.select("tr");
            for (Element element : tr) {
                if (element.text().contains("交易发生时间")){
                    continueFlag = false;
                }
                if (continueFlag || element.text().contains("本次查询共涉及") || element.text().contains("状态")){
                    continue;
                }
//                    System.out.println(element.text());
                String[] tmp = element.text().split(" ");
                AccountBill accountBill = new AccountBill();
                accountBill.time = tmp[0] + " " + tmp[1];
                accountBill.type = tmp[2];
                accountBill.shop = tmp[3];
                accountBill.turnover = tmp[5];
                accountBill.balance = tmp[6];
                try{
                    accountBill.no = Integer.parseInt(tmp[7]);
                }catch (Exception e){
                    System.out.println(tmp[7]);
                }

                accountBills.add(accountBill);
            }

            return;
        } catch (IOException e) {
            e.printStackTrace();
        }

//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                accountBillListener.done(null,e);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//
//                boolean continueFlag = true;
//                Document document= Jsoup.parse(response.body().string());
//                if (accountBills == null){
//                    accountBills = new LinkedList<>();
//                }
//
//                Elements tr=document.select("tr");
//                for (Element element : tr) {
//                    if (element.text().contains("交易发生时间")){
//                        continueFlag = false;
//                    }
//                    if (continueFlag || element.text().contains("本次查询共涉及") || element.text().contains("状态")){
//                        continue;
//                    }
////                    System.out.println(element.text());
//                    String[] tmp = element.text().split(" ");
//                    AccountBill accountBill = new AccountBill();
//                    accountBill.time = tmp[0] + " " + tmp[1];
//                    accountBill.type = tmp[2];
//                    accountBill.shop = tmp[3];
//                    accountBill.turnover = tmp[5];
//                    accountBill.balance = tmp[6];
//                    try{
//                        accountBill.no = Integer.parseInt(tmp[7]);
//                    }catch (Exception e){
//                        System.out.println(tmp[7]);
//                    }
//
//                    accountBills.add(accountBill);
//                }
//                accountBillListener.done(accountBills,null);
//                return;
//            }
//        });
    }

    /**
     * 取得continue key
     * */
    private String getContinueKey(String content){
        Pattern pattern = Pattern.compile("__continue=[0-9a-z]{32}");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            return matcher.group(0);
        }
        return "";
    }

    public static class AccountBill{
        /**
         * 次数
         * */
        public int no;

        /**
         * 交易时间
         * */
        public String time;

        /**
         * 交易类型
         * */
        public String type;

        /**
         * 商户名称
         * */
        public String shop;

        /**
         * 交易额
         * */
        public String turnover;

        /**
         * 余额
         * */
        public String balance;

        @Override
        public String toString() {
            return no + " " + time + " " + type + " \n" + shop + " " + turnover + " " + balance;
        }
    }

}




