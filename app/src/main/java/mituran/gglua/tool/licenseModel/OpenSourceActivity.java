package mituran.gglua.tool.licenseModel;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import mituran.gglua.tool.R;
import mituran.gglua.tool.licenseModel.LicenseAdapter;
import mituran.gglua.tool.licenseModel.LicenseInfo;

public class OpenSourceActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LicenseAdapter adapter;
    private List<LicenseInfo> licenseList;
    private ImageView ivBack;
    private TextView tvTitle;
    private TextView tvSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置沉浸式状态栏
        setStatusBarTransparent();

        setContentView(R.layout.activity_open_source_license);

        initViews();
        initData();
        initRecyclerView();
    }

    private void setStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_licenses);
        ivBack = findViewById(R.id.iv_back);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);

        ivBack.setOnClickListener(v -> onBackPressed());
    }

    private void initData() {
        licenseList = new ArrayList<>();

        // 添加开源库示例数据
        licenseList.add(new LicenseInfo(
                "sora-editor",
                "Rosemoe",
                "0.0.0",
                "GNU Lesser General Public",
                "Copyright (C) 2020-2024  Rosemoe"+
                        "This library is free software; you can redistribute it and/or"+
                        "modify it under the terms of the GNU Lesser General Public"+
                        "License as published by the Free Software Foundation; either"+
                        "version 2.1 of the License, or (at your option) any later version." + "This library is distributed in the hope that it will be useful," +
                        "but WITHOUT ANY WARRANTY; without even the implied warranty of"+
                        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU"+
                        "Lesser General Public License for more details."+
                        "You should have received a copy of the GNU Lesser General Public"+
                        "License along with this library; if not, write to the Free Software"+
                        "Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA"+
                        "Please contact Rosemoe by email 2073412493@qq.com if you need"+
                        "additional information or have any questions",
                "https://github.com/Rosemoe/sora-editor"
        ));

        licenseList.add(new LicenseInfo(
                "unluac",
                "Thomas Klaeger",
                "0.0.0",
                "",
                "Copyright (c) 2011-2020 tehtmi"+
                        "With Portions Copyright (c) 2014 Thomas Klaeger"+
                        " Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: " + "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software."+"THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.",
                "https://github.com/Jeong-Min-Cho/unluac"
        ));
        licenseList.add(new LicenseInfo(
                "aXML",
                "APK Explorer & Editor",
                "0.0.0",
                "GPL",
                "Copyright (C) 2023-2026 APK Explorer & Editor <apkeditor@protonmail.com>\n" +
                        "\n" +
                        "aXML is free software: you can redistribute it and/or modify it\n" +
                        "under the terms of the GNU General Public License as published by\n" +
                        "the Free Software Foundation, either version 3 of the License,\n" +
                        "or (at your option) any later version.\n" +
                        "\n" +
                        "aXML is distributed in the hope that it will be useful,\n" +
                        "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
                        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.\n" +
                        "See the GNU General Public License for more details.\n" +
                        "\n" +
                        "You should have received a copy of the GNU General Public License\n" +
                        "along with this program. If not, see <https://www.gnu.org/licenses/>.",
                "https://github.com/apk-editor/aXML"
        ));
        licenseList.add(new LicenseInfo(
                "ArscEditor",
                "ha1vk",
                "0.0.0",
                "Apache-2.0",
                " Copyright [yyyy] [name of copyright owner]\n" +
                        "\n" +
                        "   Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                        "   you may not use this file except in compliance with the License.\n" +
                        "   You may obtain a copy of the License at\n" +
                        "\n" +
                        "       http://www.apache.org/licenses/LICENSE-2.0\n" +
                        "\n" +
                        "   Unless required by applicable law or agreed to in writing, software\n" +
                        "   distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                        "   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                        "   See the License for the specific language governing permissions and\n" +
                        "   limitations under the License.",
                "https://github.com/Jeong-Min-Cho/unluac"
        ));

/*        licenseList.add(new LicenseInfo(
                "apktool",
                "Ryszard Wiśniewski",
                "0.0.0",
                "Apache License",
                        "Copyright 2010 Ryszard Wiśniewski\n" +
                        "\n" +
                        "Licensed under the Apache License, Version 2.0 (the \"License\"); you may not use this file except in compliance with the License. You may obtain a copy of the License at\n" +
                        "\n" +
                        "   https://www.apache.org/licenses/LICENSE-2.0\n" +
                        "Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License."
                ,"https://github.com/iBotPeaches/Apktool/"
        ));*/

        // 更新副标题显示库的数量
        tvSubtitle.setText("本应用使用了 " + licenseList.size() + " 个开源项目");
    }

    private void initRecyclerView() {
        adapter = new LicenseAdapter(this, licenseList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 添加滚动监听实现头部阴影效果
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                View header = findViewById(R.id.header_container);
                if (recyclerView.computeVerticalScrollOffset() > 0) {
                    header.setElevation(8f);
                } else {
                    header.setElevation(0f);
                }
            }
        });
    }
}