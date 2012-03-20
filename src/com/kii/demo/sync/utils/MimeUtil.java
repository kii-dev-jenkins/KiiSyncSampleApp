package com.kii.demo.sync.utils;

import java.util.HashMap;

import android.text.TextUtils;

import com.kii.demo.sync.R;
import com.kii.sync.KiiFile;

public class MimeUtil {

    static HashMap<String, MimeInfo> mimeInfos = new HashMap<String, MimeInfo>();
    static HashMap<String, MimeInfo> fileIcons = new HashMap<String, MimeInfo>();

    static {
        fileIcons.put("zip", new MimeInfo(R.drawable.icons_format_zip,
                "application/zip"));
        fileIcons.put("pdf", new MimeInfo(R.drawable.icons_format_pdf,
                "application/pdf"));

        fileIcons.put("txt", new MimeInfo(R.drawable.icon_format_text,
                "text/plain"));
        fileIcons.put("bas", new MimeInfo(R.drawable.icon_format_text,
                "text/plain"));
        fileIcons.put("c", new MimeInfo(R.drawable.icon_format_text,
                "text/plain"));
        fileIcons.put("h", new MimeInfo(R.drawable.icon_format_text,
                "text/plain"));

        fileIcons.put("htm", new MimeInfo(R.drawable.icon_format_markup,
                "text/html"));
        fileIcons.put("html", new MimeInfo(R.drawable.icon_format_markup,
                "text/html"));
        fileIcons.put("stm", new MimeInfo(R.drawable.icon_format_markup,
                "text/html"));

        fileIcons.put("doc", new MimeInfo(R.drawable.icons_format_doc,
                "application/msword"));
        fileIcons.put("dot", new MimeInfo(R.drawable.icons_format_doc,
                "application/msword"));

        fileIcons.put("xla", new MimeInfo(R.drawable.icons_format_xls,
                "application/vnd.ms-excel"));
        fileIcons.put("xlc", new MimeInfo(R.drawable.icons_format_xls,
                "application/vnd.ms-excel"));
        fileIcons.put("xlm", new MimeInfo(R.drawable.icons_format_xls,
                "application/vnd.ms-excel"));
        fileIcons.put("xls", new MimeInfo(R.drawable.icons_format_xls,
                "application/vnd.ms-excel"));
        fileIcons.put("xlt", new MimeInfo(R.drawable.icons_format_xls,
                "application/vnd.ms-excel"));
        fileIcons.put("xlw", new MimeInfo(R.drawable.icons_format_xls,
                "application/vnd.ms-excel"));

        fileIcons.put("ppt", new MimeInfo(R.drawable.icons_format_ppt,
                "application/vnd.ms-powerpoint"));
        fileIcons.put("pot", new MimeInfo(R.drawable.icons_format_ppt,
                "application/vnd.ms-powerpoint"));
        fileIcons.put("pps", new MimeInfo(R.drawable.icons_format_ppt,
                "application/vnd.ms-powerpoint"));

        fileIcons.put("swf", new MimeInfo(R.drawable.icon_format_flash,
                "application/x-shockwave-flash"));

        fileIcons.put("apk", new MimeInfo(R.drawable.icon_format_jar,
                "application/vnd.android.package-archive"));
        fileIcons.put("jar", new MimeInfo(R.drawable.icon_format_jar,
                "application/vnd.android.package-archive"));
        fileIcons.put("jad", new MimeInfo(R.drawable.icon_format_jar,
                "application/vnd.android.package-archive"));

        fileIcons.put("jpg", new MimeInfo(R.drawable.icon_format_images,
                "image/jpeg"));
        fileIcons.put("jpeg", new MimeInfo(R.drawable.icon_format_images,
                "image/jpeg"));
        fileIcons.put("png", new MimeInfo(R.drawable.icon_format_images,
                "image/png"));
        fileIcons.put("gif", new MimeInfo(R.drawable.icon_format_images,
                "image/gif"));
        fileIcons.put("tif", new MimeInfo(R.drawable.icon_format_images,
                "image/tiff"));
        fileIcons.put("tiff", new MimeInfo(R.drawable.icon_format_images,
                "image/tiff"));
        fileIcons.put("bm", new MimeInfo(R.drawable.icon_format_images,
                "image/bmp"));
        fileIcons.put("bmp", new MimeInfo(R.drawable.icon_format_images,
                "image/bmp"));

        fileIcons.put("3gp", new MimeInfo(R.drawable.icon_format_video,
                "video/3gpp"));
        fileIcons.put("3g2", new MimeInfo(R.drawable.icon_format_video,
                "video/3gpp"));
        fileIcons.put("mp4", new MimeInfo(R.drawable.icon_format_video,
                "video/mp4"));
        fileIcons.put("wmv", new MimeInfo(R.drawable.icon_format_video,
                "video/wmv"));
        fileIcons.put("avi", new MimeInfo(R.drawable.icon_format_video,
                "video/avi"));
        fileIcons.put("xvid", new MimeInfo(R.drawable.icon_format_video,
                "video/x-divx"));
        fileIcons.put("webm", new MimeInfo(R.drawable.icon_format_video,
                "video/webm"));
        fileIcons.put("m4v", new MimeInfo(R.drawable.icon_format_video,
                "video/x-m4v"));

        fileIcons.put("au", new MimeInfo(R.drawable.icon_format_audio,
                "audio/basic"));
        fileIcons.put("snd", new MimeInfo(R.drawable.icon_format_audio,
                "audio/basic"));
        fileIcons.put("mid", new MimeInfo(R.drawable.icon_format_audio,
                "audio/midi"));
        fileIcons.put("midi", new MimeInfo(R.drawable.icon_format_audio,
                "audio/midi"));
        fileIcons.put("rmi", new MimeInfo(R.drawable.icon_format_audio,
                "audio/midi"));
        fileIcons.put("mp3", new MimeInfo(R.drawable.icon_format_audio,
                "audio/mpeg"));
        fileIcons.put("aif", new MimeInfo(R.drawable.icon_format_audio,
                "audio/x-aiff"));
        fileIcons.put("aifc", new MimeInfo(R.drawable.icon_format_audio,
                "audio/x-aiff"));
        fileIcons.put("aiff", new MimeInfo(R.drawable.icon_format_audio,
                "audio/x-aiff"));
        fileIcons.put("m3u", new MimeInfo(R.drawable.icon_format_audio,
                "audio/x-mpegurl"));
        fileIcons.put("ra", new MimeInfo(R.drawable.icon_format_audio,
                "audio/x-pn-realaudio"));
        fileIcons.put("ram", new MimeInfo(R.drawable.icon_format_audio,
                "audio/x-pn-realaudio"));
        fileIcons.put("wav", new MimeInfo(R.drawable.icon_format_audio,
                "audio/wav"));
        fileIcons.put("wma", new MimeInfo(R.drawable.icon_format_audio,
                "audio/x-ms-wma"));
    }

    public static MimeInfo getInfoByExt(String fileExtension) {
        return fileIcons.get(fileExtension.toLowerCase());
    }

    public static MimeInfo getInfoByFileName(String fileName) {
        String suffix = getSuffixOfFile(fileName);
        if (suffix != null) {
            return fileIcons.get(suffix);
        }
        return null;
    }

    public static MimeInfo getInfoByKiiFile(KiiFile file) {
        String title = file.getTitle();
        String mimeType = file.getMimeType();
        if (!TextUtils.isEmpty(mimeType)) {
            return MimeUtil.getInfoByMimeName(mimeType);
        } else if (!TextUtils.isEmpty(mimeType)) {
            return MimeUtil.getInfoByFileName(title);
        }
        return null;
    }

    public static MimeInfo getInfoByMimeName(String mimeType) {
        String mime = mimeType.toLowerCase();
        return mimeInfos.get(mime);
    }

    public static String getSuffixOfFile(String fileName) {
        String suffix = null;
        if (!TextUtils.isEmpty(fileName)) {
            int dotPos = fileName.lastIndexOf(".");
            if (dotPos >= 0) {
                suffix = fileName.substring(dotPos + 1).toLowerCase();
            }
        }
        return suffix;
    }

}
