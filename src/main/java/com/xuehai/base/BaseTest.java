package com.xuehai.base;

import com.alibaba.fastjson.*;
import com.xuehai.model.Entity;
import com.xuehai.model.MockDTO;
import com.xuehai.model.TestCase;
import com.xuehai.utils.AssertionUtil;
import com.xuehai.utils.CommonUtil;
import com.xuehai.utils.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.mockserver.client.MockServerClient;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.qameta.allure.Allure.*;

/**
 * @ClassName BaseTest
 * @Description: 测试基础类
 * @Author Sniper
 * @Date 2019/3/13 16:55
 */
public class BaseTest {

    private static final String CLASS_NAME = BaseTest.class.getName();
    private static HttpClientDispatch client;
    private TestCase testCase;

    /**
     * @description:            初始化TestCase和HttpClient
     * @param context           ITestContext
     * @return void
     * @author Sniper
     * @date 2019/3/15 17:17
     */
    @BeforeClass
    protected void beforeBaseClass(ITestContext context) {
        Log.info(CLASS_NAME, "测试类{}开始执行", getClass());
        Map casePathMap = (Map) Configuration.getConfig().get("case-path");
        String suiteName = context.getSuite().getName();
        Log.info(CLASS_NAME, "获取测试用例路径,当前SuiteName:{}", suiteName);
        String casePath = (String) casePathMap.get(suiteName);
        testCase = parseTestCase(casePath, getClass().getName());
        Log.info(CLASS_NAME, "{}({})测试用例加载成功", testCase.getName(), getClass().getName());
        if (client == null) {
            client = HttpClientDispatch.getInstance();
        }
    }

    /**
     * @description:    测试结束打印日志
     * @return void
     * @author Sniper
     * @date 2019/4/19 10:03
     */
    @AfterClass
    protected void afterBaseClass(){
        Log.info(CLASS_NAME, "测试类: {} 执行完毕", getClass());
    }

    /**
     * @description:            发送http请求,并验证响应数据
     * @param entity            请求实体
     * @param assertionMap      测试类断言Map<String, AssertionHandler>
     * @return void
     * @author Sniper
     * @date 2019/4/22 14:57
     */
    protected void sendHttpRequest(ITestContext context, Entity entity, HashMap<String, AssertionHandler> assertionMap) {
        try {
            assertion(sendHttpRequest(context, entity), assertionMap, new Assertion(entity.getAssertion()));
        } catch (JSONException e) {
            Log.error(CLASS_NAME, "测试用例断言失败,Assertion实例化出错", e);
        }
    }

    /**
     * @description:                发送http请求
     * @param entity                请求实体
     * @return java.lang.String
     * @throws
     * @author Sniper
     * @date 2019/4/18 10:50
     */
    protected String sendHttpRequest(ITestContext context, Entity entity) {
        JSONObject contextJson = new JSONObject();
        Set<String> attrName = context.getAttributeNames();
        for (String name : attrName) {
            Object value  = context.getAttribute(name);
            contextJson.put(name, value);
        }
        description(entity.getDescription());
        parameter("ITestContext: ", contextJson.toJSONString());
        parameter("Entity: ", JSON.toJSONString(entity));
        return client.sendHttpRequest(context, entity);
    }

    /**
     * @description:    获取TestCase实体
     * @param filePath  测试用例文件路径
     * @param className 测试用例对应的测试类类名
     * @return          返回TestCase实体
     * @author Sniper
     * @date 2019/3/13 17:02
     */
    protected TestCase parseTestCase(String filePath, String className) {
        Log.info(CLASS_NAME, "开始加载测试用例,用例文件路径:{},待加载用例ClassName:{}", filePath, className);
        TestCase testCase = null;
        try {
            if (!StringUtils.isEmpty(filePath)) {
                String testCaseJson = FileUtil.read(filePath, "UTF-8");
                String jsonPath = "$[className='" + className + "']";
                JSONArray testCases = (JSONArray) JSONPath.read(testCaseJson, jsonPath);
                if (testCases.size() > 1) {
                    Log.warn(CLASS_NAME, "存在{}条类名为{}的测试用例,默认取最后一条", testCases.size(), className);
                }
                for (TestCase targetTestCase : testCases.toJavaList(TestCase.class)) {
                    testCase = targetTestCase;
                }
            } else {
                throw new NullPointerException("测试用例加载失败,文件路径为空");
            }
        } catch (JSONException e) {
            Log.error(CLASS_NAME, "测试用例加载失败", e);
        }
        return testCase;
    }


    /**
     * @description:    测试数据初始化
     * @return java.util.Iterator<java.lang.Object[]>
     * @throws
     * @author Sniper
     * @date 2019/4/22 14:45
     */
    protected Iterator<Object[]> initData() {
        List<Object[]> dataList = new ArrayList<>();
        List<Map<String, Entity>> list = testCase.getEntityList();
        list.forEach(map -> dataList.add(new Object[]{map}));
        return dataList.iterator();
    }

    /**
     * @description:    响应断言
     * @param responseDTO            实际响应数据
     * @param assertionMap        断言Map<action名称, AssertionHandler实现类>
     * @param assertion           断言数据
     * @return void
     * @throws
     * @author Sniper
     * @date 2019/4/19 10:03
     */
    protected void assertion(String responseDTO, HashMap<String, AssertionHandler> assertionMap, Assertion assertion) {
        try {
            if (responseDTO != null) {
                JSONObject actualResponseDTO = JSONObject.parseObject(responseDTO);
                AssertionUtil.assertion("ResponseCode校验", actualResponseDTO.getIntValue("responseCode"),
                        assertion.responseCode());
                Object actualResponse = actualResponseDTO.get("response");
                String action = assertion.action();
                if (!StringUtils.isEmpty(action)) {
                    assertionMap.get(assertion.action()).assertion(actualResponse, assertion);
                } else {
                    Object expectResponse = assertion.response();
                    AssertionUtil.assertion(actualResponse, expectResponse, assertion);
                }
            } else {
                throw new NullPointerException("用例断言失败,接口响应信息为空");
            }
        } catch (JSONException e) {
            Log.error(CLASS_NAME, "用例断言失败", e);
        }
    }

}
