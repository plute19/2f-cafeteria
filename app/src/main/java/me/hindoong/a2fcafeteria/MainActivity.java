package me.hindoong.a2fcafeteria;

import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    public static Context mainContext;

    //[START util 관련 문서]
    Calendar calendar;
    SimpleDateFormat mSimpleDateFormat;
    String today; // 오늘 일자
    String selectedDate; // 유저가 선택한 일자
    HashMap<String, String> week;
    //[END util 관련 문서]

    //[START view 관련 변수]
    LinearLayout ll_sv;
    ScrollView sv;
    CoordinatorLayout wrapper;
    TableLayout tl_today;
    HashMap<String, TableLayout> tables;
    TableRow tr;
    TextView tv_menuname, tv_menupop, tv_categorypop, tv_date;
    Button bt_good, bt_bad, signInOut;
    Context context = MainActivity.this;
    CalendarView cv;
    //[END view 관련 변수]

    // [START 구글 계정 로그인 관련 변수]
    private static final String TAG = "GoogleActivity";
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    // [END declare_auth]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // [START 변수 초기화]
        mainContext = this;
        // [END 변수 초기화]

        // [START View 연결]
        tv_date = findViewById(R.id.tv_date);
        cv = findViewById(R.id.calendar);
        tl_today = findViewById(R.id.table_today);
        sv = findViewById(R.id.sv);
        wrapper = findViewById(R.id.wrapper);
        cv = findViewById(R.id.calendar);
        ll_sv = findViewById(R.id.ll_sv);
        signInOut = findViewById(R.id.signInOut);
        // [END View 연결]

        // [START] 날짜 데이터 저장
        calendar = Calendar.getInstance();
        mSimpleDateFormat = new SimpleDateFormat ( "yyyyMMdd", Locale.KOREA );
        today = mSimpleDateFormat.format ( calendar.getTime() );
        selectedDate = today;
        tv_date.setText("일자 선택 -> " + selectedDate.substring(0,4) + ". "
                + selectedDate.substring(4,6) + ". " + selectedDate.substring(6,8));
        week = new HashMap<>();
        getDate(selectedDate);
        tables = new HashMap<>();
        for (String key : week.keySet()) {
            int layoutid = getResources().getIdentifier(
                    "table_" + key, "id", "me.hindoong.a2fcafeteria"
            );
            tables.put(key,(TableLayout)findViewById(layoutid));
        }
        // [END] 날짜 데이터 저장

        // [START 달력 관련]
        cv.setVisibility(View.GONE);
        cv.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView calendarView, int i, int i1, int i2) {
                calendar.set(i,i1,i2);
                selectedDate = mSimpleDateFormat.format(calendar.getTime());
                tv_date.setText("일자 선택 -> " + selectedDate.substring(0,4) + ". "
                        + selectedDate.substring(4,6) + ". " + selectedDate.substring(6,8));
                calendarToggle(calendarView);
                getDate(selectedDate);
                for (String key : week.keySet()) {
                    int layoutid = getResources().getIdentifier(
                            "table_" + key, "id", "me.hindoong.a2fcafeteria"
                    );
                    tables.put(key,(TableLayout)findViewById(layoutid));
                }
                String result = Util.getInstance().request(getString(R.string.url), "getSet", week);
                jsonParse(result);
            }
        });
        // [END 달력 관련]

        // [START 서버 연결 관련]
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        // [END 서버 연결 관련]

        // [START 구글 로그인 관련]
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // initialize_auth
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            signInOut.setText("로그인");
        } else {
            signInOut.setText("로그아웃");
        }

        // 관리자 여부 검증
        if (Util.getInstance().isManager(getString(R.string.url))) {
            Button goManagePage = new Button(context);
            goManagePage.setText("관리자 페이지");
            goManagePage.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, ManageActivity.class);
                    startActivity(intent);
                }
            });
            ll_sv.addView(goManagePage);
        }
        // [END 구글 로그인 관련]

        // [START 식단표 가져오기]
        resetTableLayout();
        String result = Util.getInstance().request(getString(R.string.url), "getSet", week);
        jsonParse(result);
        // [END 식단표 가져오기]

    }

    // 가져온 json을 화면에 보여주기
    public void jsonParse(String page){
        JSONArray jarray;
        JSONObject item;

        resetTableLayout();

        if (page.length() == 0) return;

        try {

            JSONObject jo = new JSONObject(page);

            for (String key : week.keySet()) {

                String date = week.get(key);

                try {
                    jarray = jo.getJSONArray(date);
                } catch (JSONException e) {
                    //해당하는 날짜의 식단표가 없는 경우...
                    continue;
                }

                for (int i = 0; i < jarray.length(); i++) {
                    item = jarray.getJSONObject(i);
                    String menuName = item.getString("menuname");
                    String menuPop = item.getInt("total")==0?
                            "평가 없음":Math.round((item.getDouble("good")/item.getDouble("total"))*100)+"%";
                    String categoryPop = item.getString("categoryname") + "/";
                    categoryPop += item.getInt("categorytotal")==0?
                            "평가 없음":Math.round((item.getDouble("categorygood")/item.getDouble("categorytotal"))*100)+"%";

                    tr = new TableRow(context);
                    tr.setWeightSum(4);
                    tr.setOrientation(LinearLayout.HORIZONTAL);

                    tv_menuname = new TextView(context);
                    tv_menuname.setLayoutParams(new TableRow.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
                    tv_menuname.setText(menuName);
                    tv_menuname.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                    tv_menupop = new TextView(context);
                    tv_menupop.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
                    tv_menupop.setText(menuPop);
                    tv_menupop.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                    tv_categorypop = new TextView(context);
                    tv_categorypop.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
                    tv_categorypop.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    tv_categorypop.setText(categoryPop);

                    tr.addView(tv_menuname);
                    tr.addView(tv_menupop);
                    tr.addView(tv_categorypop);

                    tables.get(key).addView(tr);

                    if (date.equals(selectedDate)) {

                        TableRow today_row = new TableRow(context);
                        today_row.setWeightSum(6);

                        TextView tv_menuname_today = new TextView(context);
                        tv_menuname_today.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 2f));
                        tv_menuname_today.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        tv_menuname_today.setText(menuName);

                        TextView tv_menupop_today = new TextView(context);
                        tv_menupop_today.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
                        tv_menupop_today.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        tv_menupop_today.setText(menuPop);

                        TextView tv_categorypop_today = new TextView(context);
                        tv_categorypop_today.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
                        tv_categorypop_today.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        tv_categorypop_today.setText(categoryPop);

                        today_row.addView(tv_menuname_today);
                        today_row.addView(tv_menupop_today);
                        today_row.addView(tv_categorypop_today);

                        if (date.equals(today)) {

                            bt_good = new Button(context);
                            bt_good.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
                            bt_good.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            bt_good.setTag("g" + item.getInt("menunum"));
                            bt_good.setText("좋음");

                            bt_bad = new Button(context);
                            bt_bad.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
                            bt_bad.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            bt_bad.setTag("b" + item.getInt("menunum"));
                            bt_bad.setText("별로");

                            today_row.addView(bt_good);
                            today_row.addView(bt_bad);

                            bt_good.setOnClickListener(new ButtonClickListener());
                            bt_bad.setOnClickListener(new ButtonClickListener());
                        }

                        tl_today.addView(today_row);
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 좋음/별로 클릭 시 처리
    private class ButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View view) {

            if (mAuth.getCurrentUser() == null) {
                //로그인 정보 없음 -> 로그인 안내
                signInOut(view);
                return;
            }

            String tag = (String) view.getTag();
            int menunum = Integer.parseInt(tag.substring(1, tag.length()));
            String memberemail = mAuth.getCurrentUser().getEmail();
            int rate = tag.charAt(0) == 'g' ? 1 : 0;

            HashMap<String, String> rateMap = new HashMap<>();
            rateMap.put("ratedate", today);
            rateMap.put("menunum", menunum + "");
            rateMap.put("rate", rate + "");
            rateMap.put("memberemail", memberemail);

            String result = Util.getInstance().request(getString(R.string.url), "insertRate", rateMap);
            MainActivity.this.recreate();
            Toast.makeText(context, "반영되었습니다", Toast.LENGTH_SHORT).show();
        }
    }


    // [START 구글 계정으로 로그인 관련 메소드]

    public void signInOut(View view) {

        if (mAuth.getCurrentUser() == null) {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);

        } else{
            mAuth.signOut();
            mGoogleSignInClient.signOut();
            Toast.makeText(MainActivity.this, "로그아웃 됨", Toast.LENGTH_SHORT).show();
            this.recreate();
        }

    }

    // onActivityResult
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);

            } catch (ApiException e) {

                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }

    // auth_with_google
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        // [START_EXCLUDE silent]
        showProgressDialog();
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            ((MainActivity)MainActivity.mainContext).recreate();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Snackbar.make(wrapper, "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                        }

                        // [START_EXCLUDE]
                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
    }

    // [END 구글 계정으로 로그인 관련 메소드]


    // [START UTIL 메소드]

    //날짜를 입력 받아 해당 주의 일자를 월요일을 기준으로 해서 배열 week에 저장
    private void getDate(String yyyymmdd) {

        String[] key = {"mon", "tue", "wed", "thu", "fri"};

        int yy = Integer.parseInt(yyyymmdd.substring(0, 4));
        int mm = Integer.parseInt(yyyymmdd.substring(4, 6)) - 1;
        int dd = Integer.parseInt(yyyymmdd.substring(6, 8));

        calendar.set(yy, mm, dd);

        int inDay = calendar.get(Calendar.DAY_OF_MONTH);
        int yoil = calendar.get(Calendar.DAY_OF_WEEK)-2; //요일나오게하기(숫자로)

        inDay = inDay-yoil;

        for(int i = 0; i < 5;i++){
            calendar.set(yy, mm, inDay+i);
            week.put(key[i],mSimpleDateFormat.format(calendar.getTime()));
        }

    }

    private void resetTableLayout() {
        tl_today.removeAllViews();
        for (String key : week.keySet()) {
            tables.get(key).removeAllViews();
        }
    }

    public void calendarToggle(View view) {

        int i = cv.getVisibility();

        if (i == 8) {
            cv.setVisibility(View.VISIBLE);
        } else {
            cv.setVisibility(View.GONE);
        }

    }

}
