package com.ceabie.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import demo.ceabie.com.demo.R;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private TextView mViewById;
    private long mMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewById = (TextView) findViewById(R.id.text);

        findViewById(R.id.btn_save_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int dfg = 121111;
                Observable.just(1, 2, dfg)
                        .map(new Func1<Integer, Integer>() {
                            @Override
                            public Integer call(Integer integer) {
                                return integer + 10;
                            }
                        })
//                        .flatMap(new Func1<Integer, Observable<Integer>>() {
//                            @Override
//                            public Observable<Integer> call(Integer integer) {
//                                return Observable.just(integer + 10);
//                            }
//                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Action1<Integer>() {
                            @Override
                            public void call(Integer integer) {
                                log(String.valueOf(integer));
                            }
                        });
            }
        });
    }


    private void log(final String stext) {
        mViewById.setText(mViewById.getText().toString() + "\n" + stext);
    }
}
