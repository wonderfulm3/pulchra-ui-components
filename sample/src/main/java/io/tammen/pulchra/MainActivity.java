package io.tammen.pulchra;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import io.tammen.pulchra.sample.R;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.demo_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.demo_button:
                Log.d(TAG, "Popover example");
                new CustomTooltip.Builder(this)
                        .anchorView(view)
                        .text("Popover content goes here. Views, etc")
                        .gravity(Gravity.END)
                        .animated(false)
                        .contentView(R.layout.tooltip_custom)
                        .dismissOnInsideTouch(false)
                        .dismissOnOutsideTouch(true)
                        .build()
                        .show();
                break;
            default:
                break;
        }
    }
}
