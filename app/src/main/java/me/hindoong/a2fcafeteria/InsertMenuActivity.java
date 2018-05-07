package me.hindoong.a2fcafeteria;

import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class InsertMenuActivity extends BaseActivity {

    //[START view 관련 변수]
    CoordinatorLayout wrapper;
    EditText et_menuname;
    Context context = InsertMenuActivity.this;
    Button bt_insert, bt_goAddMenu;
    Spinner spinner;
    //[END view 관련 변수]

    HashMap<String, String> categoryMap;
    String categoryname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insertmenu);

        // 관리자 여부 검증
        Util util = Util.getInstance();
        if (!util.isManager(getString(R.string.url))) {
            finish();
        }

        // [START 뷰 연결]
        wrapper = findViewById(R.id.wrapper);
        et_menuname = findViewById(R.id.et_menuname);
        bt_insert = findViewById(R.id.bt_insert);
        spinner = findViewById(R.id.spinner);
        bt_goAddMenu = findViewById(R.id.bt_goAddMenu);
        // [END 뷰 연결]

        bt_goAddMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // [START 변수 초기화]
        categoryMap = new HashMap<>();
        // [END 변수 초기화]

        // [START 서버 연결 관련]
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        // [END 서버 연결 관련]

        String result = Util.getInstance().request(getString(R.string.url), "selectCategory", null);
        jsonParse(result);

    }

    // 가져온 json을 화면에 보여주기
    public void jsonParse(String page){
        JSONArray jarray = null;
        JSONObject item;

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line);

        try {

            jarray = new JSONArray(page);

            for (int i = 0; i < jarray.length(); i++) {

                item = jarray.getJSONObject(i);
                String categoryNum = item.getInt("categorynum")+"";
                String categoryName = item.getString("categoryname");

                categoryMap.put(categoryName, categoryNum);

                arrayAdapter.add(categoryName);

            }

            spinner.setAdapter(arrayAdapter);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void insertMenu(View view) {

        String menuname = et_menuname.getText().toString();
        categoryname = spinner.getSelectedItem().toString();

        String categorynum = categoryMap.get(categoryname);

        HashMap<String, String> map = new HashMap<>();
        map.put("menuname", menuname);
        map.put("categorynum", categorynum);

        String result = Util.getInstance().request(getString(R.string.url), "insertMenu", map);
        if (result.equals("success")) {
            Toast.makeText(context, "등록 완료", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "이미 존재하는 메뉴입니다", Toast.LENGTH_SHORT).show();
        }

    }

}
