package com.demo.epaper.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.demo.epaper.R;
import com.demo.epaper.activity.MainActivity;
import com.demo.epaper.adapter.TemplateAdapter;
import com.demo.epaper.databinding.FragmentTagBinding;
import com.demo.epaper.entity.AirCondition;
import com.demo.epaper.entity.City;
import com.demo.epaper.entity.CalendarInfo;
import com.demo.epaper.entity.RecentWeather;
import com.demo.epaper.entity.TodayWeather;
import com.demo.epaper.entity.ForecastWeather;
import com.demo.epaper.entity.WeatherTemplate;
import com.demo.epaper.handler.BluetoothListener;
import com.demo.epaper.handler.HttpCallback;
import com.demo.epaper.handler.ItemClickListener;
import com.demo.epaper.imageio.WeatherImageGenerator;
import com.demo.epaper.network.BleTransceiver;
import com.demo.epaper.network.HttpUtil;
import com.demo.epaper.thread.SingleThreadPool;
import com.demo.epaper.thread.HttpThread;
import com.demo.epaper.utils.AppUtils;
import com.demo.epaper.utils.NetConst;
import com.demo.epaper.utils.SqliteHelper;
import com.demo.epaper.view.EpaperView;
import com.demo.epaper.view.LoadingDialog;
import com.demo.epaper.view.PixelView;
import com.demo.epaper.view.TransferDialog;
import com.demo.epaper.view.QToast;
import com.demo.epaper.view.VerticalDecoration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TagFragment extends Fragment {
    private final Activity activity;
    private FragmentTagBinding binding;

    private static final int CITY_SELECTED_SIZE = 2;
    private List<City> cities;
    private List<City> displayCities;
    private String cityPinyin;

    private List<WeatherTemplate> weatherTemplates;
    private TemplateAdapter templateAdapter;

    private AtomicInteger atomicCount;
    private SingleThreadPool singleThreadPool;
    private List<ForecastWeather> forecasts;
    private CalendarInfo calendarInfo;
    private TodayWeather todayWeather;
    private RecentWeather recentWeather;
    private AirCondition airCondition;
    private boolean[] cachedPage;

    private LoadingDialog loadingDialog;
    private WeatherImageGenerator imageGenerator;

    private BleTransceiver bleTransceiver;
    private TransferDialog transferDialog;
    private byte[] frameBuffer;
    private int mtu;
    private AtomicInteger atomicOffset;

    private static final int DEVICE_ACTIVE_CODE = 200;
    private int isDeviceActive;

    private String filesDir;

    public TagFragment(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTagBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initParam();

        float density = activity.getResources().getDisplayMetrics().density;
        float vSplit = AppUtils.dp2Px(density,10);

        RecyclerView recyclerTemplate = binding.recyclerTemplate;

        templateAdapter = new TemplateAdapter(activity, weatherTemplates);
        LinearLayoutManager manager2 = new LinearLayoutManager(activity.getApplicationContext());
        manager2.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerTemplate.setLayoutManager(manager2);
        recyclerTemplate.addItemDecoration(new VerticalDecoration((int)vSplit));

        templateAdapter.setItemClickListener(templateListener);
        recyclerTemplate.setAdapter(templateAdapter);
    }

    private void initParam() {
        isDeviceActive = 0;
        cities = new ArrayList<>(48);
        displayCities = new ArrayList<>(48);

        filesDir = activity.getExternalFilesDir(null).getAbsolutePath();
        SqliteHelper helper = new SqliteHelper(filesDir);
        helper.listProvince(cities);

        SharedPreferences preferences = activity.getSharedPreferences("app_env", Context.MODE_PRIVATE);
        cityPinyin = preferences.getString("city", null);

        if(cityPinyin == null) {
            displayCities.addAll(cities);
        }else {
            City currentCity = helper.getProvinceByUrbanPinyin(cityPinyin);
            if(currentCity != null) {
                for(City city : cities) {
                    if(city.getChildId().equals(currentCity.getParentId())) {
                        city.setSelect(true);
                        currentCity.setSelect(true);
                        displayCities.add(0, city);
                        displayCities.add(1, currentCity);
                        break;
                    }
                }
            }
        }
        helper.close();

        weatherTemplates = new ArrayList<>(8);
        weatherTemplates.add(new WeatherTemplate("选择城市", 0xFF838BC5, WeatherTemplate.TITLE_BAR));
        WeatherTemplate templateCity = new WeatherTemplate(WeatherTemplate.CITY_SELECTOR);
        templateCity.setCitySelection(cities, displayCities);
        weatherTemplates.add(templateCity);

        weatherTemplates.add(new WeatherTemplate("页面样式", 0xff7bbfea, WeatherTemplate.TITLE_BAR));
        WeatherTemplate template1 = new WeatherTemplate(WeatherTemplate.TEMPLATE);
        template1.setTitle("天气预报");
        template1.setSubTitle("最近3天的气象预报和公历、农历信息。");
        template1.setResId(R.mipmap.ic_weather_forecast);
        weatherTemplates.add(template1);

        WeatherTemplate template2 = new WeatherTemplate(WeatherTemplate.TEMPLATE);
        template2.setTitle("今日天气");
        template2.setSubTitle("今日的天气、空气质量和日期显示。");
        template2.setResId(R.mipmap.ic_weather_today);
        weatherTemplates.add(template2);

        weatherTemplates.add(new WeatherTemplate("发送到标签",0xff45b97c, WeatherTemplate.USER_BUTTON));

        bleTransceiver = BleTransceiver.getInstance();
        atomicCount = new AtomicInteger(0);
        cachedPage = new boolean[2];
    }

    private static final int MSG_UPDATE_PROGRESS = 200;
    private static final int MSG_DISMISS_DIALOG = 201;

    private static final int MSG_REQUEST_ALL_DONE = 202;
    private static final int FORECAST_PREVIEW = 0;
    private static final int TODAY_PREVIEW = 1;

    private static final int MSG_REQUEST_FAILED = 203;

    private static final int MSG_GET_ACTIVE_STATE = 204;

    private Handler handler = new Handler((@NonNull Message msg) -> {
        if(msg.what == MSG_UPDATE_PROGRESS) {
            transferDialog.setProgress(msg.arg1);

        }else if(msg.what == MSG_DISMISS_DIALOG) {
            transferDialog.setProgress(0);
            transferDialog.dismiss();

        }else if(msg.what == MSG_REQUEST_ALL_DONE) {
            loadingDialog.dismiss();
            if(imageGenerator == null) {
                imageGenerator = new WeatherImageGenerator(filesDir);
            }
            cachedPage[msg.arg1] = true;
            if(msg.arg1 == FORECAST_PREVIEW) {
                imageGenerator.generateForecast(todayWeather, forecasts, calendarInfo);
                int[] pixels = imageGenerator.getInnerBuffer();
                binding.epaperPreview.setPixels(pixels, 250, 122);
            }else if(msg.arg1 == TODAY_PREVIEW) {
                imageGenerator.generateToday(todayWeather, airCondition, recentWeather, calendarInfo);
                int[] pixels = imageGenerator.getInnerBuffer();
                binding.epaperPreview.setPixels(pixels, 250, 122);
            }
        }else if(msg.what == MSG_REQUEST_FAILED) {
            loadingDialog.dismiss();
            showToast("网络请求失败", QToast.COLOR_ORANGE, false);

        }else if(msg.what == MSG_GET_ACTIVE_STATE) {
            if(msg.arg1 == 0x1) {
                isDeviceActive = DEVICE_ACTIVE_CODE;
                actionUpload();
            }else {
                isDeviceActive = -1;
                showToast("请升级新版固件", QToast.COLOR_ORANGE, false);
            }
        }
        return true;
    });

    private final HttpCallback httpCallback = (int id, int code, String body) -> {
        if((code != HttpUtil.STATE_OK) || (body == null)) {
            showToast("网络请求异常", QToast.COLOR_ORANGE, true);
            return;
        }
        Document document = Jsoup.parse(body);
        if(id == 100) {
            Element element = document.getElementById("mobile3");
            if(element != null) {
                if(forecasts == null) {
                    forecasts = new ArrayList<>(ForecastWeather.FORECAST_MAX);
                }
                forecasts.clear();

                Elements elementDay = element.getElementsByClass("wtbg");
                Elements elementWeather = element.getElementsByClass("wtwt");
                Elements elementWind = element.getElementsByClass("wtwind");
                Elements elementTemp = element.getElementsByClass("wttemp");

                int temperature, low, high;
                for(int i = 0; i < ForecastWeather.FORECAST_MAX; i++) {
                    temperature = temperatureDescDecode(elementTemp.get(i).text());
                    low = (temperature & 0xFFFF);
                    high = ((temperature & 0xFFFF0000) >> 16);
                    forecasts.add(new ForecastWeather(elementDay.get(i).text(), elementWeather.get(i).text(),
                            elementWind.get(i).text(), low, high));
                }
                atomicCount.incrementAndGet();
            }
        }else if(id == 101) {
            Element element = document.getElementById("mobile7");
            if(element != null) {
                Elements elementLunar = element.getElementsByClass("wtwind");
                String[] array = elementLunar.get(0).text().split(" ");
                calendarInfo = new CalendarInfo(array[1], array[2], array[3], array[4]);

                int hour, minute;
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
                if (calendar.get(Calendar.AM_PM) == Calendar.AM) {
                    hour = calendar.get(Calendar.HOUR);
                } else {
                    hour = calendar.get(Calendar.HOUR) + 12;
                }
                minute = calendar.get(Calendar.MINUTE);
                calendarInfo.setTime(calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH), hour, minute);
                atomicCount.incrementAndGet();
            }
        }else if(id == 102) {
            Elements elementBox1 = document.getElementsByClass("box1");
            Elements elementBox2 = document.getElementsByClass("box2");
            Elements elementBox3 = document.getElementsByClass("box3");

            String[] array;
            todayWeather = new TodayWeather();
            array = elementBox1.get(0).text().split(" ");
            todayWeather.setTemperature(dumpInteger(array[0]));
            todayWeather.setCityName(array[1]);

            array = elementBox2.get(0).text().split(" ");
            todayWeather.setDateDesc(array[0]);
            todayWeather.setWeekDesc(array[1]);

            array = elementBox3.get(0).text().split(" ");
            String[] subArray = array[0].split("：");
            todayWeather.setHumidityDesc(subArray[1]);
            subArray = array[1].split("：");
            todayWeather.setUvDesc(subArray[1]);

            atomicCount.incrementAndGet();
        }else if(id == 103) {
            Elements elementName = document.getElementsByClass("wtname");
            String city = elementName.get(0).text();
            Element elementDay1 = document.getElementById("day_1");
            Element elementDay2 = document.getElementById("day_2");
            Element elementDay3 = document.getElementById("day_3");
            if((elementDay1 == null) || (elementDay2 == null) || (elementDay3 == null)) {
                return;
            }
            recentWeather = new RecentWeather(
                    city.substring(0, (city.length() - 4)),
                    elementDay1.text(), elementDay2.text(), elementDay3.text());
            atomicCount.incrementAndGet();
        }else if(id == 104) {
            Elements elementLinks = document.getElementsByTag("a");
            String condition = null;
            for(Element e : elementLinks) {
                condition = e.attr("title");
                if(condition.startsWith("今日")) {
                    break;
                }
                condition = null;
            }
            if(condition != null) {
                int exp = dumpInteger(condition.substring(10));
                int index = condition.indexOf("\t") + 1;
                airCondition = new AirCondition(exp, condition.substring(index));
                atomicCount.incrementAndGet();
            }
        }
    };

    private int lastClick = -1;
    private static final int BUTTON_UPLOAD = 5;
    private static final int TEMPLATE_START = 3;

    private final ItemClickListener templateListener = (int type, int position) -> {
        if(position == BUTTON_UPLOAD) {
            if(atomicCount.get() > 0) {
                if(!isConnectionValid()) {
                    return;
                }
                if(isDeviceActive < 0) {
                    showToast("请升级新版固件", QToast.COLOR_ORANGE, false);
                }else if(isDeviceActive < DEVICE_ACTIVE_CODE) {
                    actionReadDeviceActiveState();
                }else {
                    actionUpload();
                }
            }else {
                showToast("请先选择页面样式", QToast.COLOR_ORANGE, false);
            }
            return;
        }
        if(displayCities.size() != CITY_SELECTED_SIZE) {
            showToast("请先选择城市", QToast.COLOR_ORANGE, false);
            return;
        }
        if((cityPinyin == null) || (!cityPinyin.equals(displayCities.get(1).getPinyin()))) {
            cityPinyin = displayCities.get(1).getPinyin();
            saveCitySelection(cityPinyin);
            Arrays.fill(cachedPage, false);
        }

        if(lastClick == position) {
            return;
        }
        if(lastClick == -1) {
            weatherTemplates.get(position).setChecked(true);
            templateAdapter.notifyItemChanged(position, "UPDATE");
        }else {
            weatherTemplates.get(lastClick).setChecked(false);
            weatherTemplates.get(position).setChecked(true);
            int count = Math.abs(lastClick - position) + 1;
            templateAdapter.notifyItemRangeChanged(Math.min(lastClick, position), count, "UPDATE");
        }
        lastClick = position;
        if(loadingDialog == null) {
            initLoadingDialog();
        }
        if(singleThreadPool == null) {
            singleThreadPool = SingleThreadPool.getInstance();
        }
        int page = (position - TEMPLATE_START);
        if(cachedPage[page]) {
            Message message = handler.obtainMessage();
            message.what = MSG_REQUEST_ALL_DONE;
            message.arg1 = page;
            handler.sendMessage(message);
            return;
        }
        if(page == FORECAST_PREVIEW) {
            atomicCount.set(0);
            singleThreadPool.execute(new HttpThread(100,
                    "http://i.tianqi.com/index.php?c=code&a=getcode&id=3&py=" + cityPinyin, httpCallback));
            singleThreadPool.execute(new HttpThread(101,
                    "http://i.tianqi.com/index.php?c=code&a=getcode&id=7&py=" + cityPinyin, httpCallback));
            singleThreadPool.execute(new HttpThread(102,
                    "http://i.tianqi.com/index.php?c=code&a=getcode&id=102&py=" + cityPinyin, httpCallback));
            singleThreadPool.execute(() -> {
                if(atomicCount.get() == 3) {
                    Message message = handler.obtainMessage();
                    message.what = MSG_REQUEST_ALL_DONE;
                    message.arg1 = FORECAST_PREVIEW;
                    handler.sendMessage(message);
                }else {
                    handler.sendEmptyMessage(MSG_REQUEST_FAILED);
                }
            });
        }else if(page == TODAY_PREVIEW) {
            atomicCount.set(0);
            singleThreadPool.execute(new HttpThread(101,
                    "http://i.tianqi.com/index.php?c=code&a=getcode&id=7&py=" + cityPinyin, httpCallback));
            singleThreadPool.execute(new HttpThread(102,
                    "http://i.tianqi.com/index.php?c=code&a=getcode&id=102&py=" + cityPinyin, httpCallback));
            singleThreadPool.execute(new HttpThread(103,
                    "http://i.tianqi.com/index.php?c=code&a=getcode&id=8&py=" + cityPinyin, httpCallback));
            singleThreadPool.execute(new HttpThread(104,
                    "http://i.tianqi.com/index.php?c=code&a=getcode&id=50&py=" + cityPinyin, httpCallback));
            singleThreadPool.execute(() -> {
                if(atomicCount.get() == 4) {
                    Message message = handler.obtainMessage();
                    message.what = MSG_REQUEST_ALL_DONE;
                    message.arg1 = TODAY_PREVIEW;
                    handler.sendMessage(message);
                }else {
                    handler.sendEmptyMessage(MSG_REQUEST_FAILED);
                }
            });
        }
        loadingDialog.showMessage("联网中...");
    };

    private static final String[] READ_WRITE_MESSAGE = new String[]{"写入成功", "偏移量超范围",
            "数据长度超范围", "包格式错误", "价签忙碌中"};
    private static final String[] FLUSH_MESSAGE = new String[]{"传输完成", "价签忙碌中", "价签刷新完成"};

    private final BluetoothListener tagBleListener = new BluetoothListener() {
        @Override
        public void onDataReceived(UUID uuid, byte[] data, int length) {
            if(!NetConst.EPD_READ_UUID.equals(uuid)) {
                return;
            }
            byte opcode = data[NetConst.APP_RESP_ACTION_IDX];
            byte status = data[NetConst.APP_RESP_STATUS_IDX];

            if(opcode == NetConst.ACTION_WRITE_RAM) {
                if(status == NetConst.ACTION_RESULT_OK) {
                    uploadFrameBuffer();
                }else {
                    handler.sendEmptyMessage(MSG_DISMISS_DIALOG);
                    showToast(READ_WRITE_MESSAGE[status], QToast.COLOR_ORANGE, true);
                }

            }else if(opcode == NetConst.ACTION_FLUSH_DISPLAY) {
                showToast(FLUSH_MESSAGE[status], QToast.COLOR_GREEN, true);

            }else if(opcode == NetConst.ACTION_READ_CONFIG) {
                if(status != NetConst.ACTION_RESULT_OK) {
                    return;
                }
                int tag = AppUtils.bytes2Int(data, 2, AppUtils.SHORT_BYTES);
                int len = AppUtils.bytes2Int(data, 4, AppUtils.SHORT_BYTES);

                if((tag == NetConst.CONFIG_IS_ACTIVE) && (len == 1)) {
                    Message message = handler.obtainMessage();
                    message.what = MSG_GET_ACTIVE_STATE;
                    message.arg1 = AppUtils.bytes2Int(data, 6, len);
                    handler.sendMessage(message);
                }
            }
        }

        @Override
        public void onStatusChanged(int type, int data) {
            if(type == BluetoothListener.BLE_DISCONNECTED) {
                isDeviceActive = 0;
            }
        }
    };

    private void actionReadDeviceActiveState() {
        byte[] request = new byte[3];
        request[0] = NetConst.ACTION_READ_CONFIG;
        request[1] = (byte)0x01;
        request[2] = (byte)0xFF;
        bleTransceiver.writeCharacteristic(NetConst.EPD_WRITE_UUID, request);
    }

    private void actionUpload() {
        if(transferDialog == null) {
            transferDialog = new TransferDialog(activity);
        }
        transferDialog.setTitle("上传中");
        transferDialog.show();
        if(frameBuffer == null) {
            frameBuffer = new byte[EpaperView.EPD_RAM_SIZE];
            atomicOffset = new AtomicInteger();
        }
        mtu = (bleTransceiver.getMTU() - 3);
        atomicOffset.set(0);
        binding.epaperPreview.getTransformedData(frameBuffer);

        uploadFrameBuffer();
    }

    private void uploadFrameBuffer() {
        int offset = atomicOffset.get();
        int trunk = Math.min((mtu - NetConst.WRITE_RAM_HEADER_SIZE), (PixelView.EPD_RAM_SIZE - offset));
        if(trunk == 0) {
            handler.sendEmptyMessage(MSG_DISMISS_DIALOG);
            flushEpaperDisplay();
            return;
        }
        int percent = ((offset * 100) / PixelView.EPD_RAM_SIZE);
        Message message = handler.obtainMessage();
        message.what = MSG_UPDATE_PROGRESS;
        message.arg1 = percent;
        handler.sendMessage(message);

        byte[] data = new byte[mtu];
        data[0] = NetConst.ACTION_WRITE_RAM;
        AppUtils.int2Bytes(data, 1, offset, 4);
        data[5] = (byte)trunk;
        System.arraycopy(frameBuffer, offset, data, 6, trunk);

        atomicOffset.set(offset + trunk);
        bleTransceiver.writeCharacteristic(NetConst.EPD_WRITE_UUID, data);
    }

    private void flushEpaperDisplay() {
        byte[] request = new byte[]{NetConst.ACTION_FLUSH_DISPLAY};
        bleTransceiver.writeCharacteristic(NetConst.EPD_WRITE_UUID, request);
    }

    private boolean isConnectionValid() {
        MainActivity host = (MainActivity)activity;
        if(!bleTransceiver.isBLEConnected()) {
            host.showToast("请先连接蓝牙", QToast.COLOR_ORANGE, false);
            return false;
        }
        if(!bleTransceiver.hasBLEFunction(NetConst.EPD_WRITE_UUID)) {
            host.showToast("当前不支持该功能", QToast.COLOR_ORANGE, false);
            return false;
        }
        return true;
    }

    private void showToast(String msg, int type, boolean b) {
        MainActivity main = (MainActivity)activity;
        main.showToast(msg, type, b);
    }

    private void initLoadingDialog() {
        loadingDialog = new LoadingDialog(activity);
    }

    private int temperatureDescDecode(String desc) {
        int temperature = 0;
        int lowPart, highPart;
        int end = desc.indexOf('℃');
        String low = desc.substring(0, end);
        try {
            lowPart = Integer.parseInt(low);
        }catch (NumberFormatException e) {
            e.printStackTrace();
            lowPart = 0;
        }
        end = desc.indexOf('～') + 1;
        String high = desc.substring(end, desc.length() - 1);
        try {
            highPart = Integer.parseInt(high);
        }catch (NumberFormatException e) {
            e.printStackTrace();
            highPart = 0;
        }

        temperature |= (lowPart & 0xFFFF);
        temperature |= ((highPart & 0xFFFF) << 16);

        return temperature;
    }

    private int dumpInteger(String str) {
        byte[] array = str.getBytes(StandardCharsets.UTF_8);
        byte[] number = new byte[10];
        int count = 0;
        boolean flag = false;

        for (byte b : array) {
            if ((b >= 0x30) && (b <= 0x39)) {
                if (count == 0) {
                    flag = true;
                }
                number[count] = b;
                count++;
                if (count > 9) {
                    break;
                }
            } else if(flag) {
                break;
            }
        }
        int result;
        try {
            result = Integer.parseInt(new String(number, 0, count, StandardCharsets.UTF_8));
        }catch (NumberFormatException e) {
            result = 0;
        }
        return result;
    }

    private void saveCitySelection(String a) {
        SharedPreferences preferences = activity.getSharedPreferences("app_env", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("city", a);
        editor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        bleTransceiver.registerReceiveListener(tagBleListener);
    }

    @Override
    public void onPause() {
        bleTransceiver.removeReceiveListener(tagBleListener);
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(hidden) {
            isDeviceActive = 0;
            bleTransceiver.removeReceiveListener(tagBleListener);
        }else {
            bleTransceiver.registerReceiveListener(tagBleListener);
        }
    }

    @Override
    public void onDestroy() {
        binding.epaperPreview.recycle();
        binding = null;
        templateAdapter.close();
        templateAdapter = null;
        weatherTemplates.clear();
        weatherTemplates = null;
        cities.clear();
        cities = null;
        displayCities.clear();
        displayCities = null;

        if(singleThreadPool != null) {
            singleThreadPool.close();
            singleThreadPool = null;
        }
        handler.removeCallbacksAndMessages(null);
        handler = null;

        loadingDialog = null;
        if(imageGenerator != null) {
            imageGenerator.close();
            imageGenerator = null;
        }
        if(forecasts != null) {
            forecasts.clear();
            forecasts = null;
        }
        todayWeather = null;
        calendarInfo = null;
        recentWeather = null;
        airCondition = null;
        atomicCount = null;

        bleTransceiver = null;
        frameBuffer = null;
        super.onDestroy();
    }
}