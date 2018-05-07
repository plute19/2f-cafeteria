package me.hindoong.a2fcafeteria;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class ManageActivity extends BaseActivity {

    ArrayList<String> menunumList;

    //[START util 관련 문서]
    Calendar calendar;
    SimpleDateFormat mSimpleDateFormat;
    String date; // 일자
    //[END util 관련 문서]

    //[START view 관련 변수]
    ScrollView sv;
    LinearLayout wrapper;
    TableLayout tl;
    TableRow tr;
    TextView tv_menuname, tv_date;
    Button bt_delete, bt_add, bt_confirm, bt_goMain;
    Context context = ManageActivity.this;
    CalendarView cv;
    //[END view 관련 변수]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage);

        //[START View 연결]
        wrapper = findViewById(R.id.wrapper);
        menunumList = new ArrayList<>();
        sv = findViewById(R.id.sv);
        tv_date = findViewById(R.id.tv_date);
        bt_goMain = findViewById(R.id.bt_goMain);
        cv = findViewById(R.id.calendar);
        tl = findViewById(R.id.tl);
        bt_add = findViewById(R.id.bt_add);
        bt_confirm = findViewById(R.id.bt_confirm);
        //[END View 연결]

        // 관리자 여부 검증
        if (!Util.getInstance().isManager(getString(R.string.url))) {
            finish();
        }

        // [START] 오늘 날짜 저장
        calendar = Calendar.getInstance();
        mSimpleDateFormat = new SimpleDateFormat ( "yyyyMMdd", Locale.KOREA );
        date = mSimpleDateFormat.format ( calendar.getTime() );
        tv_date.setText("일자 선택 -> " + date.substring(0,4) + ". " + date.substring(4,6) + ". " + date.substring(6,8));
        menunumList.add(date);  //배열의 첫번째는 날짜로 함.
        // [END] 날짜 데이터 저장

        // [START 서버 연결 관련]
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        // [END 서버 연결 관련]

        // [START 식단표 가져오기]
        tl.setGravity(View.TEXT_ALIGNMENT_CENTER);
        setCard();
        // [END 식단표 가져오기]

        // [START 달력 관련]
        cv.setVisibility(View.GONE);
        cv.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView calendarView, int i, int i1, int i2) {
                calendar.set(i,i1,i2);
                date = mSimpleDateFormat.format(calendar.getTime());
                menunumList.set(0, date);
                tv_date.setText("일자 선택 -> " + date.substring(0,4) + ". " + date.substring(4,6) + ". " + date.substring(6,8));
                menunumList.set(0, date);
                calendarToggle(calendarView);
                setCard();
            }
        });
        // [END 달력 관련]

        // [START 버튼 관련]
        bt_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, AddMenuActivity.class);
                startActivityForResult(intent, 100);
            }
        });
        bt_goMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        // [START 버튼 관련]

    }


    // [START UTIL 메소드]

    private void resetTableLayout() {

        tl.removeAllViews();
    }

    // [END UTIL 메소드]

    // 식단표 가져오기
    public void setCard() {

        HashMap<String,String> map = new HashMap<>();
        map.put("cookdate", date);
        String result = Util.getInstance().request(getString(R.string.url), "getSet", map);
        jsonParse(result);

    }

    // 가져온 json을 화면에 보여주기
    public void jsonParse(String page){
        JSONArray jarray;
        JSONObject item;

        resetTableLayout();

        menunumList.clear();
        menunumList.add(date);

        if (page.length() == 0) return;

        try {

            jarray = new JSONArray(page);

            if (jarray.length() == 0) {
                //해당하는 날짜의 식단표가 없는 경우...
                TextView alert = new TextView(context);
                alert.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                alert.setText("등록된 식단 없음...");
                alert.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                tl.addView(alert);
                return;
            }

            for (int i = 0; i < jarray.length(); i++) {

                item = jarray.getJSONObject(i);
                String menuNum = item.getInt("menunum")+"";
                String menuName = item.getString("menuname");

                tr = new TableRow(context);
                tr.setWeightSum(6);

                tv_menuname = new TextView(context);
                tv_menuname.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 2f));
                tv_menuname.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                tv_menuname.setText(menuName);

                bt_delete = new Button(context);
                bt_delete.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
                bt_delete.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                bt_delete.setTag(menuNum);
                bt_delete.setText("삭제");

                menunumList.add(menuNum);

                tr.addView(tv_menuname);
                tr.addView(bt_delete);

                bt_delete.setOnClickListener(new deleteMenu());

                tl.addView(tr);

            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    // 삭제 버튼을 클릭한 로우를 안 보이게 하고 메뉴 배열에서 지움
    private class deleteMenu implements View.OnClickListener {
        @Override
        public void onClick(View view) {

            // 클릭한 메뉴의 넘버
            String tag = view.getTag().toString();
            menunumList.remove(tag);

            TableRow tr_tmp = (TableRow)view.getParent();
            tr_tmp.setVisibility(View.GONE);

            Log.e("menunumList", menunumList.toString());
        }
    }

    // 배열을 바탕으로 전송
    public void confirmMenu(View view) {

        HashMap<String, String> map = new HashMap<>();
        map.put("menunumList", menunumList.toString().replace("[","").replace("]",""));

        String result = Util.getInstance().request(getString(R.string.url), "insertCard", map);
        Toast.makeText(context, result + "개의 메뉴가 저장되었습니다.", Toast.LENGTH_SHORT).show();

    }

    public void calendarToggle(View view) {

        int i = cv.getVisibility();

        if (i == 8) {
            cv.setVisibility(View.VISIBLE);
        } else {
            cv.setVisibility(View.GONE);
        }

    }

    // 뒤로 가기 버튼 클릭 시 메인 액티비티의 식단표 새로 고침
    @Override
    public void onBackPressed() {

        ((MainActivity)MainActivity.mainContext).recreate();
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == 100) {
            // 메뉴 추가함...
            String menunum = data.getStringExtra("menunum");
            String menuname = data.getStringExtra("menuname");

            tr = new TableRow(context);
            tr.setWeightSum(6);

            tv_menuname = new TextView(context);
            tv_menuname.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 2f));
            tv_menuname.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv_menuname.setText(menuname);

            bt_delete = new Button(context);
            bt_delete.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
            bt_delete.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            bt_delete.setTag(menunum);
            bt_delete.setText("삭제");

            menunumList.add(menunum);

            tr.addView(tv_menuname);
            tr.addView(bt_delete);

            bt_delete.setOnClickListener(new deleteMenu());

            tl.addView(tr);

            Log.e("ee", menuname);
        }
    }
}
