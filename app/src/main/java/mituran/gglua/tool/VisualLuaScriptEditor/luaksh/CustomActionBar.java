package mituran.gglua.tool.VisualLuaScriptEditor.luaksh;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import mituran.gglua.tool.R;

public class CustomActionBar extends LinearLayout {
    private Button btnRun;
    private Button btnSave;
    private Button btnAddBlock;
    private Button btnDefineFunction;
    private Button btnClear;
    private Button btnExport;

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

        btnRun = findViewById(R.id.btn_run);
        btnSave = findViewById(R.id.btn_save);
        btnAddBlock = findViewById(R.id.btn_add_block);
        btnDefineFunction = findViewById(R.id.btn_define_function);
        btnClear = findViewById(R.id.btn_clear);
        btnExport = findViewById(R.id.btn_export);
    }

    public void setOnRunClickListener(OnClickListener listener) {
        btnRun.setOnClickListener(listener);
    }

    public void setOnSaveClickListener(OnClickListener listener) {
        btnSave.setOnClickListener(listener);
    }

    public void setOnAddBlockClickListener(OnClickListener listener) {
        btnAddBlock.setOnClickListener(listener);
    }

    public void setOnDefineFunctionClickListener(OnClickListener listener) {
        btnDefineFunction.setOnClickListener(listener);
    }

    public void setOnClearClickListener(OnClickListener listener) {
        btnClear.setOnClickListener(listener);
    }

    public void setOnExportClickListener(OnClickListener listener) {
        btnExport.setOnClickListener(listener);
    }
}