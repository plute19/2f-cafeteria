package me.hindoong.a2fcafeteria;

import com.google.firebase.auth.FirebaseAuth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by insect on 2018. 3. 9..
 */

public class Util {

    // [START 구글 계정 로그인 관련 변수]
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    // [END 구글 계정 로그인 관련 변수]

    //[START 서버 연결 관련 변수]
    java.net.URL url;
    HttpURLConnection con;
    //[END 서버 연결 관련 변수]

    private Util() {
        mAuth = FirebaseAuth.getInstance();
    }

    private static class Singleton {
        static final Util instance = new Util();
    }

    public static Util getInstance() {
        return Singleton.instance;
    }

    // 관리자 계정 여부 검증
    boolean isManager(String url) {

        boolean isManager = false;

        if (mAuth.getCurrentUser() != null) {

            String param = "manageremail=" + mAuth.getCurrentUser().getEmail();

            try {
                this.url = new URL(url + "isManager");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            try {
                con = (HttpURLConnection) this.url.openConnection();

                if (con != null) {
                    con.setConnectTimeout(10000);
                    con.setUseCaches(false);
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Accept-Charset", "UTF-8");
                    con.setRequestProperty("Context_Type", "application/x-www-form-urlencoded;cahrset=UTF-8");

                    OutputStream os = con.getOutputStream();
                    os.write(param.getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {

                        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

                        String line;
                        StringBuilder page = new StringBuilder();

                        while ((line = reader.readLine()) != null) {
                            page.append(line);
                        }

                        if (page.toString().equals("isManager")) isManager = true;

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }

        }

            return isManager;

    }

    //현재 로그인된 유저의 이메일 반환
    String getUserEmail() {

        return mAuth.getCurrentUser().getEmail();
    }

    //서버로 요청을 보내고 응답을 반환
    String request(String url, String request, HashMap<String,String> params) {
        try{
            this.url = new URL(url + request);
        } catch (MalformedURLException e){
            e.printStackTrace();
        }

        try{
            con = (HttpURLConnection) this.url.openConnection();

            String param = makeParams(params);

            if(con != null){
                con.setConnectTimeout(10000);
                con.setUseCaches(false);
                con.setRequestMethod("POST");
                con.setRequestProperty("Accept-Charset", "UTF-8");
                con.setRequestProperty("Context_Type", "application/x-www-form-urlencoded;cahrset=UTF-8");

                OutputStream os = con.getOutputStream();
                os.write(param.getBytes("UTF-8"));
                os.flush();
                os.close();

                if(con.getResponseCode() == HttpURLConnection.HTTP_OK){

                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

                    String line;
                    StringBuilder page = new StringBuilder();

                    while ((line = reader.readLine()) != null){
                        page.append(line);
                    }

                    return page.toString();

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(con != null){
                con.disconnect();
            }
        }

        return null;

    }

    //서버로 보낼 데이터를 쿼리 스트링으로 변환 ("?이름=값&이름2=값2" 형식)
    private String makeParams(HashMap<String,String> params){

        if (params == null) {
            return "";
        }

        StringBuilder sbParam = new StringBuilder();
        String key;
        String value;
        boolean isAnd = false;

        for(Map.Entry<String,String> elem : params.entrySet()){
            key = elem.getKey();
            value = elem.getValue();

            if(isAnd){
                sbParam.append("&");
            }

            sbParam.append(key).append("=").append(value);

            if(!isAnd){
                if(params.size() >= 2){
                    isAnd = true;
                }
            }
        }

        return sbParam.toString();
    }


}
