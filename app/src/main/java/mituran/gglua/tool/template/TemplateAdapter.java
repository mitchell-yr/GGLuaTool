package mituran.gglua.tool.template;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import mituran.gglua.tool.R;
import mituran.gglua.tool.model.Template;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {

    private List<Template> templates;
    private Activity activity;

    public TemplateAdapter(List<Template> templates, Activity activity) {
        this.templates = templates;
        this.activity = activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Template template = templates.get(position);

        holder.nameText.setText(template.name);
        holder.versionText.setText("版本: " + template.version);

        holder.manageButton.setOnClickListener(v -> showManageMenu(v, template));
        holder.editButton.setOnClickListener(v -> editTemplate(template));
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }



    private void showManageMenu(View view, Template template) {
        PopupMenu popup = new PopupMenu(activity, view);
        popup.getMenuInflater().inflate(R.menu.menu_manage, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
                deleteTemplate(template);
                return true;
            } else if (id == R.id.action_export) {
                exportTemplate(template);
                return true;
            } else if (id == R.id.action_rename) {
                renameTemplate(template);
                return true;
            } else if (id == R.id.action_copy) {
                copyTemplate(template);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void deleteTemplate(Template template) {
        new AlertDialog.Builder(activity)
                .setTitle("删除模板")
                .setMessage("确定要删除模板 \"" + template.name + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    if (template.file.delete()) {
                        templates.remove(template);
                        notifyDataSetChanged();
                        Toast.makeText(activity, "删除成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void exportTemplate(Template template) {
        // 这里可以实现导出功能，比如分享文件
        Toast.makeText(activity, "导出功能待实现", Toast.LENGTH_SHORT).show();
    }

    private void renameTemplate(Template template) {
        EditText editText = new EditText(activity);
        editText.setText(template.name);

        new AlertDialog.Builder(activity)
                .setTitle("重命名模板")
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equals(template.name)) {
                        File newFile = new File(template.file.getParent(), newName + ".json");
                        if (template.file.renameTo(newFile)) {
                            template.name = newName;
                            template.file = newFile;
                            notifyDataSetChanged();
                            Toast.makeText(activity, "重命名成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity, "重命名失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void copyTemplate(Template template) {
        String newName = template.name + "_副本";
        File newFile = new File(template.file.getParent(), newName + ".json");

        try {
            FileInputStream fis = new FileInputStream(template.file);
            FileOutputStream fos = new FileOutputStream(newFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fis.close();
            fos.close();

            ((TemplateManagerActivity) activity).loadTemplates();
            Toast.makeText(activity, "复制成功", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(activity, "复制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void editTemplate(Template template) {
        Intent intent = new Intent(activity, TemplateEditorActivity.class);
        intent.putExtra("template_name", template.name);
        intent.putExtra("template_version", template.version);
        intent.putExtra("template_code", template.code);
        activity.startActivityForResult(intent, 2002);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView versionText;
        Button manageButton;
        Button editButton;

        ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.templateName);
            versionText = view.findViewById(R.id.templateVersion);
            manageButton = view.findViewById(R.id.manageButton);
            editButton = view.findViewById(R.id.editButton);
        }
    }
}