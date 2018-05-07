package me.hindoong.a2fcafeteria;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class AddMenuActivity extends BaseActivity {

    //[START view 관련 변수]
    ScrollView sv;
    LinearLayout wrapper, ll_searchResult;
    EditText et_search;
    ArrayList<TextView> list_tv_result;
    ArrayList<HashMap<String,String>> list_map_result;
    Context context = AddMenuActivity.this;
    Button bt_add, bt_remove, bt_goManage;
    int menunum;
    String menuname;
    //[END view 관련 변수]

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addmenu);

        // 관리자 여부 검증
        Util util = Util.getInstance();
        if (!util.isManager(getString(R.string.url))) {
            finish();
        }

        // [START 뷰 연결]
        wrapper = findViewById(R.id.wrapper);
        sv = findViewById(R.id.sv);
        ll_searchResult = findViewById(R.id.ll_searchResult);
        et_search = findViewById(R.id.et_search);
        bt_add = findViewById(R.id.bt_add);
        bt_remove = findViewById(R.id.bt_remove);
        bt_goManage = findViewById(R.id.bt_goManage);
        // [END 뷰 연결]

        // [START 변수 초기화]
        list_tv_result = new ArrayList<>();
        list_map_result = new ArrayList<>();
        // [END 변수 초기화]

        // [START 서버 연결 관련]
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        // [END 서버 연결 관련]

        // [START 리스너 연결]
        // 검색창 리스너 연결
        et_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                bt_add.setVisibility(View.GONE);
                bt_remove.setVisibility(View.GONE);
                HashMap<String, String> map = new HashMap<>();
                map.put("menuname", et_search.getText().toString());
                String result = Util.getInstance().request(getString(R.string.url),"selectMenuList", map);
                jsonParse(result);
            }
        });

        bt_goManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        // [END 리스너 연결]

    }

    // [START UTIL 메소드]

    private void cleanSearchResult() {

        ll_searchResult.removeAllViews();
    }

    // [END UTIL 메소드]

    // 가져온 json을 화면에 보여주기
    public void jsonParse(String page){
        JSONArray jarray;
        JSONObject item;

        cleanSearchResult();

        list_tv_result.clear();
        list_map_result.clear();

        try {

            jarray = new JSONArray(page);

            if (jarray.length() == 0) {
                // 검색 결과가 없는 경우 -> 새로운 메뉴 등록 유도
                return;
            }

            for (int i = 0; i < jarray.length(); i++) {

                item = jarray.getJSONObject(i);
                String menuNum = item.getInt("menunum")+"";
                String menuName = item.getString("menuname");

                TextView tv_result = new TextView(context);
                tv_result.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tv_result.setBackgroundColor(getColor(R.color.colorAccent));
                tv_result.setTextColor(Color.WHITE);
                tv_result.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                tv_result.setText(menuName);
                tv_result.setTag(menuNum);
                tv_result.setBottom(1);
                tv_result.setOnClickListener(new ClickListener());

                ll_searchResult.addView(tv_result);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 검색 결과 클릭...
    private class ClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {

            String tag = (String)view.getTag();
            TextView tv = (TextView)view;
            menunum = Integer.parseInt(tag);
            menuname = tv.getText().toString();

            et_search.setText(menuname);

            ll_searchResult.removeAllViews();

            bt_add.setVisibility(View.VISIBLE);
            bt_remove.setVisibility(View.VISIBLE);

        }
    }

    public void addMenu(View view) {
        Intent intent = new Intent(context, ManageActivity.class);
        intent.putExtra("menunum", menunum+"");
        intent.putExtra("menuname", menuname);
        setResult(100, intent);
        finish();
    }

    public void removeMenu(View view) {
        HashMap<String, String> map = new HashMap<>();
        map.put("menunum", menunum+"");
        String result = Util.getInstance().request(getString(R.string.url), "removeMenu", map);
        if (result.equals("success")) {
            Toast.makeText(context,"삭제되었습니다", Toast.LENGTH_SHORT).show();
            et_search.setText("");
        } else {
            Toast.makeText(context,"문제가 발생했습니다...", Toast.LENGTH_SHORT).show();
        }

    }

    public void insertMenu(View view) {
        Intent intent = new Intent(context, InsertMenuActivity.class);
        startActivity(intent);
    }

}
