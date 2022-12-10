package com.karl.network.vue;

import android.content.Context;

import com.karl.network.NetWorkApplication;
import com.yanzhenjie.andserver.annotation.Controller;
import com.yanzhenjie.andserver.annotation.GetMapping;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okio.Okio;

@Controller
public class PageController {

    @GetMapping(path = "/")
    public String index() throws IOException {
        InputStream open = NetWorkApplication.Companion.getContext().getAssets().open("xcs-app-web/index.html");
        String s = Okio.buffer(Okio.source(open)).readUtf8();
        File directory = new File("");

        return "forward:/index.html";
    }
}
