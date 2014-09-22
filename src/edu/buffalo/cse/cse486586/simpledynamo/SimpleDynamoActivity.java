package edu.buffalo.cse.cse486586.simpledynamo;

import android.app.Activity;
import android.content.ContentResolver;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;
import edu.buffalo.cse.cse486586.simpledynamo.listeners.OnCountClickListener;
import edu.buffalo.cse.cse486586.simpledynamo.listeners.OnDeleteClickListener;
import edu.buffalo.cse.cse486586.simpledynamo.listeners.OnDumpClickListener;

public class SimpleDynamoActivity extends Activity {

    EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ContentResolver contentResolver = getContentResolver();

        setContentView(R.layout.activity_simple_dynamo);

        TextView textView = (TextView) findViewById(R.id.textView1);
        textView.setMovementMethod(new ScrollingMovementMethod());


        editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button3).setOnClickListener(new OnTestClickListener(textView, contentResolver, this));
        findViewById(R.id.button1).setOnClickListener(new OnDumpClickListener(textView, contentResolver, "@"));
        findViewById(R.id.button2).setOnClickListener(new OnDumpClickListener(textView, contentResolver, "*"));
        findViewById(R.id.button4).setOnClickListener(new OnCountClickListener(textView, contentResolver));
        findViewById(R.id.button5).setOnClickListener(new OnDeleteClickListener(textView, contentResolver, this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.simple_dynamo, menu);
        return true;
    }

    public String getEditText() {
        final String text = editText.getText().toString();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.setText("");
            }
        });
        return text;
    }


    public Boolean hasText() {
        return !"".equals(editText.getText().toString());
    }
}
