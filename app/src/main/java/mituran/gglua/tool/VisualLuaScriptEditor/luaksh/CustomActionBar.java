package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import mituran.gglua.tool.R;

public class CustomActionBar extends LinearLayout {
    private Button btnDefineFunction;
    private ImageButton btnRun;
    private ImageButton btnSave;
    private ImageButton btnEdit;
    private ImageButton btnSettings;

    public CustomActionBar(Context context) {
        super(context);
        init(context);
    }

    public CustomActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.visual_lua_script_editor_custom_action_bar, this, true);

        btnDefineFunction = findViewById(R.id.btn_define_function);
        btnRun = findViewById(R.id.btn_run);
        btnSave = findViewById(R.id.btn_save);
        btnEdit = findViewById(R.id.btn_edit);
        btnSettings = findViewById(R.id.btn_settings);
    }

    public void setOnDefineFunctionClickListener(OnClickListener listener) {
        btnDefineFunction.setOnClickListener(listener);
    }

    public void setOnRunClickListener(OnClickListener listener) {
        btnRun.setOnClickListener(listener);
    }

    public void setOnSaveClickListener(OnClickListener listener) {
        btnSave.setOnClickListener(listener);
    }

    public void setOnEditClickListener(OnClickListener listener) {
        btnEdit.setOnClickListener(listener);
    }

    public void setOnSettingsClickListener(OnClickListener listener) {
        btnSettings.setOnClickListener(listener);
    }
}
