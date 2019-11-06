import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author YaningLiu
 * @date 2018/9/16/ 12:05
 */
public class AliCheckTest {
    @Test
    public void zfbCheck() throws AlipayApiException {
        Configs.init("zfbinfo.properties");
        Map map = new HashMap();
        StringTokenizer items;
        String resultInfo = "body=订单:1492091096400购买商品共6598.00（元）&subject=happymmall扫码支付，订单号：1492091096400&sign_type=RSA2&buyer_logon_id=tar***@sandbox.com&auth_app_id=2016091800537532&notify_type=trade_status_sync&out_trade_no=1492091096400&point_amount=0.00&version=1.0&fund_bill_list=[{\"amount\":\"6598.00\",\"fundChannel\":\"ALIPAYACCOUNT\"}]&buyer_id=2088102176536111&total_amount=6598.00&trade_no=2018091621001004110500287791&notify_time=2018-09-16 00:37:46&charset=utf-8&invoice_amount=6598.00&trade_status=TRADE_SUCCESS&gmt_payment=2018-09-16 00:37:45&sign=eVsLCZpPC2otOZOjYtr0DZghahMRWkw/erYDyl8IZ9p1L4cIYOmA0MCfVgt5G1QtWerb/2W01ePFmXrnV1WHZuBP1soz2LzzyDFwhuorfvsGzbZb+XP+b7oNYXWJtPiFl4JXRKdgfqKmxSLRTWlUQ1L1gwccpKpazqj5J1WV0M2zaCKW0sjX8SCALmfmo01c5ofGrbaac+iGATeNWzxi5vixosxZ7b8oRscTZcOp8nKN+PTW6IEgGOQ4s+k4bljQZN5kqFmcQ+s4MaA0YbQyzroT9hl93UJroqk5uUE+ooc2ouZVEJ3lOyrYuzmyB+hMw0+3vM1WXx6AwaPY25dSoA==&gmt_create=2018-09-16 00:37:32&buyer_pay_amount=6598.00&receipt_amount=6598.00&app_id=2016091800537532&seller_id=2088102176136971&notify_id=1cda409bcb55168db677e9b11c1ae98gup&seller_email=afcdmc7411@sandbox.com";

        // 使用这个for循环，特殊字符会被忽略掉。特殊字符会被忽略掉。特殊字符会被忽略掉。
        for (StringTokenizer entrys = new StringTokenizer(resultInfo, "&");
             entrys.hasMoreTokens(); map.put(items.nextToken(), items.hasMoreTokens() ? ((items.nextToken())) : null)) {
            items = new StringTokenizer(entrys.nextToken(), "=");
        }
        System.out.println(map.get("sign"));;
        map.put("sign", "eVsLCZpPC2otOZOjYtr0DZghahMRWkw/erYDyl8IZ9p1L4cIYOmA0MCfVgt5G1QtWerb/2W01ePFmXrnV1WHZuBP1soz2LzzyDFwhuorfvsGzbZb+XP+b7oNYXWJtPiFl4JXRKdgfqKmxSLRTWlUQ1L1gwccpKpazqj5J1WV0M2zaCKW0sjX8SCALmfmo01c5ofGrbaac+iGATeNWzxi5vixosxZ7b8oRscTZcOp8nKN+PTW6IEgGOQ4s+k4bljQZN5kqFmcQ+s4MaA0YbQyzroT9hl93UJroqk5uUE+ooc2ouZVEJ3lOyrYuzmyB+hMw0+3vM1WXx6AwaPY25dSoA==");
        map.remove("sign_type");

        org.junit.Assert.assertEquals(true, AlipaySignature.rsaCheckV2(map, Configs.getAlipayPublicKey(), "utf-8", Configs.getSignType()));
    }
}
