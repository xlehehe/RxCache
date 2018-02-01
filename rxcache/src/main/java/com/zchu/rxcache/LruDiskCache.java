package com.zchu.rxcache;

import com.jakewharton.disklrucache.DiskLruCache;
import com.zchu.rxcache.diskconverter.IDiskConverter;
import com.zchu.rxcache.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

/**
 * Created by Z.Chu on 2016/9/10.
 */
class LruDiskCache {
    private IDiskConverter mDiskConverter;
    private DiskLruCache mDiskLruCache;


    LruDiskCache(IDiskConverter diskConverter, File diskDir, int appVersion, long diskMaxSize) {
        this.mDiskConverter = diskConverter;
        try {
            mDiskLruCache = DiskLruCache.open(diskDir, appVersion, 2, diskMaxSize);
        } catch (IOException e) {
            LogUtils.log(e);
        }
    }

    <T> CacheHolder<T> load(String key, Type type) {
        if (mDiskLruCache == null) {
            return null;
        }
        try {
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
            if (snapshot != null) {
                InputStream source = snapshot.getInputStream(0);
                T value = mDiskConverter.load(source, type);
                long timestamp = 0;
                String string = snapshot.getString(1);
                if (string != null) {
                    timestamp = Long.parseLong(string);
                }
                snapshot.close();
                return new CacheHolder<>(value,timestamp);
            }
        } catch (IOException e) {
            LogUtils.log(e);
        }
        return null;
    }


    <T> boolean save(String key, T value) {
        if (mDiskLruCache == null) {
            return false;
        }
        //如果要保存的值为空,则删除
        if (value == null) {
            return remove(key);
        }
        DiskLruCache.Editor edit = null;
        try {
            edit = mDiskLruCache.edit(key);
            OutputStream sink = edit.newOutputStream(0);
            mDiskConverter.writer(sink, value);
            long l = System.currentTimeMillis();
            edit.set(1, String.valueOf(l));
            edit.commit();
            return true;
        } catch (IOException e) {
            LogUtils.log(e);
            if (edit != null) {
                try {
                    edit.abort();
                } catch (IOException e1) {
                    LogUtils.log(e1);
                }
            }
        }
        return false;
    }


    boolean containsKey(String key) {
        try {
            return mDiskLruCache.get(key) != null;
        } catch (IOException e) {
            LogUtils.log(e);
        }
        return false;
    }

    /**
     * 删除缓存
     */
    final boolean remove(String key) {
        try {
            return mDiskLruCache.remove(key);
        } catch (IOException e) {
            LogUtils.log(e);
        }
        return false;
    }

    void clear() throws IOException {
        mDiskLruCache.delete();

    }


}
