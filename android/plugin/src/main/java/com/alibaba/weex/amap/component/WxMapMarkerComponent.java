package com.alibaba.weex.amap.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.alibaba.weex.amap.Constant;
import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.common.Constants;
import com.taobao.weex.dom.WXDomObject;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXVContainer;
import com.taobao.weex.utils.WXUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Created by budao on 2017/2/9.
 */

public class WxMapMarkerComponent extends WXComponent<View> {
  private Marker mMarker;
  private MapView mMapView;
  private AMap mAMap;

  public WxMapMarkerComponent(WXSDKInstance instance, WXDomObject dom, WXVContainer parent, String instanceId, boolean isLazy) {
    super(instance, dom, parent, instanceId, isLazy);
  }

  public WxMapMarkerComponent(WXSDKInstance instance, WXDomObject dom, WXVContainer parent, boolean isLazy) {
    super(instance, dom, parent, isLazy);
  }

  public WxMapMarkerComponent(WXSDKInstance instance, WXDomObject dom, WXVContainer parent) {
    super(instance, dom, parent);

  }

  @Override
  protected View initComponentHostView(@NonNull Context context) {
    if (getParent() != null && getParent() instanceof WXMapViewComponent) {
      mMapView = ((WXMapViewComponent) getParent()).getHostView();
      mAMap = mMapView.getMap();
      String title = (String) getDomObject().getAttrs().get(Constant.Name.TITLE);
      String icon = (String) getDomObject().getAttrs().get(Constant.Name.ICON);
      String position = getDomObject().getAttrs().get(Constant.Name.POSITION).toString();
      initMarker(title, position, icon);
    }
    // FixMe： 只是为了绕过updateProperties中的逻辑检查
    return new View(context);
  }

//  public void updateProperties(Map<String, Object> props) {
//    if (props == null) {
//      return;
//    }
//
//    for(String key : props.keySet()) {
//      Object param = props.get(key);
//      String value = WXUtils.getString(param, null);
//      if (TextUtils.isEmpty(value)) {
//        param = convertEmptyProperty(key);
//      }
//      if(!setProperty(key, param)){
//        Invoker invoker = mHolder.getPropertyInvoker(key);
//        if (invoker != null) {
//          try {
//            Type[] paramClazzs = invoker.getParameterTypes();
//            if (paramClazzs.length != 1) {
//              WXLogUtils.e("[WXComponent] setX method only one parameter：" + invoker);
//              return;
//            }
//            param = WXReflectionUtils.parseArgument(paramClazzs[0],props.get(key));
//            invoker.invoke(this, param);
//          } catch (Exception e) {
//            WXLogUtils.e("[WXComponent] updateProperties :" + "class:" + getClass() + "method:" + invoker.toString() + " function " + WXLogUtils.getStackTrace(e));
//          }
//        }
//      }
//    }
//  }

  @Override
  protected boolean setProperty(String key, Object param) {
    switch (key) {
      case Constants.Name.POSITION:
        String position = WXUtils.getString(param,null);
        if (position != null)
          setPosition(position);
        return true;
    }
    return super.setProperty(key, param);
  }

  @WXComponentProp(name = Constant.Name.TITLE)
  public void setTitle(String title) {
    setMarkerTitle(title);
  }

  @WXComponentProp(name = Constant.Name.ICON)
  public void setIcon(String icon) {
    setMarkerIcon(icon);
  }

  @WXComponentProp(name = Constant.Name.POSITION)
  public void setPosition(String position) {
    setMarkerPosition(position);
  }

  @Override
  public void destroy() {
    super.destroy();
    if (mMarker != null) {
      mMarker.remove();
    }
  }

  public Marker getMarker() {
    return mMarker;
  }

  public void onClick() {
    getInstance().fireEvent(getRef(), Constants.Event.CLICK);
  }

  private void initMarker(String title, String position, String icon) {
    final MarkerOptions markerOptions = new MarkerOptions();
    //设置Marker可拖动
    markerOptions.draggable(true);
    // 将Marker设置为贴地显示，可以双指下拉地图查看效果
    markerOptions.setFlat(true);
    mMarker = mAMap.addMarker(markerOptions);
    setMarkerTitle(title);
    setMarkerPosition(position);
    setMarkerIcon(icon);
  }

  private void setMarkerIcon(final String icon) {
    if (!TextUtils.isEmpty(icon)) {
      new AsyncTask<Void, String, Uri>() {

        @Override
        protected Uri doInBackground(Void... params) {
          try {
            return getImageUri(icon, getContext().getExternalCacheDir());
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }

        @Override
        protected void onPostExecute(Uri result) {
          if (result != null && new File(result.getPath()).exists()) {
            if (isGif(result.getPath())) {
              Log.v("setMarkerIcon" , "this icon is a gif file");
              GifDecoder gifDecoder = new GifDecoder();
              FileInputStream imgFile = null;
              try {

                imgFile = new FileInputStream(result.getPath());
                gifDecoder.read(imgFile);
                ArrayList<BitmapDescriptor> bitmapDescriptors = new ArrayList<BitmapDescriptor>();
                for (int i = 1; i < gifDecoder.getFrameCount(); i++) {
                  Bitmap bitmap = gifDecoder.getFrame(i);
                  if (bitmap != null && !bitmap.isRecycled()) {
                    Log.v("setMarkerIcon" , "this icon is a gif file " + i);
                    bitmapDescriptors.add(BitmapDescriptorFactory.fromBitmap(bitmap));
                  }
                }
                mMarker.setIcons(bitmapDescriptors);
                mMarker.setPeriod(2);

              } catch (FileNotFoundException e) {
                e.printStackTrace();
              } catch (IOException e) {
                e.printStackTrace();
              } finally {
                if (imgFile != null) {
                  try {
                    imgFile.close();
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }
              }

            } else {
              mMarker.setIcon(BitmapDescriptorFactory.fromPath(result.getPath()));
            }

          }
        }
      }.execute();
    }

//    if (!TextUtils.isEmpty(icon)) {
//      IWXImgLoaderAdapter adapter = WXSDKManager.getInstance().getIWXImgLoaderAdapter();
//      ImageView imageView = new ImageView(getContext()); // ImageView imageView = new ImageView(getContext());
//      imageView.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
//      imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
//      if (adapter != null) {
//        WXImageStrategy wxImageStrategy = new WXImageStrategy();
//        wxImageStrategy.setImageListener(new WXImageStrategy.ImageListener() {
//          @Override
//          public void onImageFinish(String url, ImageView imageView, boolean result, Map extra) {
//            imageView.setLayoutParams(
//                new ViewGroup.LayoutParams(
//                    ViewGroup.LayoutParams.WRAP_CONTENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT));
//            mMarker.setIcon(BitmapDescriptorFactory.fromView(imageView));
//          }
//        });
//        wxImageStrategy.placeHolder = icon;
//        adapter.setImage(icon, imageView, WXImageQuality.NORMAL, wxImageStrategy);
//      }
//    }
  }

  private void setMarkerPosition(String position) {
    try {
      JSONArray jsonArray = new JSONArray(position);
      LatLng latLng = new LatLng(jsonArray.optDouble(1), jsonArray.optDouble(0));
      mMarker.setPosition(latLng);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private void setMarkerTitle(String title) {
    mMarker.setTitle(title);
  }

  private Uri getImageUri(String path, File cache) {
    String name = Uri.encode(path);
    File file = new File(cache, name);
    // 如果图片存在本地缓存目录，则不去服务器下载
    if (file.exists()) {
      return Uri.fromFile(file);
    } else {
      // 从网络上获取图片
      InputStream inputstream = null;
      FileOutputStream fileOutputStream = null;
      try {
        HttpURLConnection conn = (HttpURLConnection) new URL(path).openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        if (conn.getResponseCode() == 200) {
          inputstream = conn.getInputStream();
          fileOutputStream = new FileOutputStream(file);
          byte[] buffer = new byte[1024];
          int len = 0;
          while ((len = inputstream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, len);
          }
          inputstream.close();
          fileOutputStream.close();
          // 返回一个URI对象
          return Uri.fromFile(file);
        }
      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (ProtocolException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (inputstream != null) {
          try {
            inputstream.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        if (fileOutputStream != null) {
          try {
            fileOutputStream.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

    }
    return null;
  }

  private String getMd5(String str) {
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      byte[] bs = md5.digest(str.getBytes());
      StringBuilder sb = new StringBuilder(40);
      for(byte x:bs) {
        if((x & 0xff)>>4 == 0) {
          sb.append("0").append(Integer.toHexString(x & 0xff));
        } else {
          sb.append(Integer.toHexString(x & 0xff));
        }
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static boolean isGif(String file) {
    FileInputStream imgFile = null;
    try {
      imgFile = new FileInputStream(file);
      byte[] header = new byte[3];
      int length = imgFile.read(header);
      return length == 3 && header[0] == (byte) 'G' && header[1] == (byte) 'I' && header[2] == (byte) 'F';
    } catch (Exception e) {
      // ignore
    } finally {
      if (imgFile != null) {
        try {
          imgFile.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return false;
  }
}
