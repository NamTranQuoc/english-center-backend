package com.englishcenter.report;

import com.englishcenter.core.utils.ResponseDomain;
import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.log.LogApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController(value = "/report")
public class ReportController extends ResponseUtils {
    @Autowired
    private ReportApplication reportApplication;
    @Autowired
    private LogApplication logApplication;

    @GetMapping("/report/count_member")
    public ResponseDomain countMember() {
        try {
            return this.outJsonV2(9999, null, reportApplication.countMember().orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/report/statistical_by_register")
    public ResponseDomain statisticalByRegister() {
        try {
            return this.outJsonV2(9999, null, reportApplication.statisticalByRegister().orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/report/statistical_by_paid")
    public ResponseDomain statisticalByPaid() {
        try {
            return this.outJsonV2(9999, null, reportApplication.statisticalByPaid().orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/report/get_recent")
    public ResponseDomain getAll() {
        try {
            return this.outJsonV2(9999, null, logApplication.getRecent().orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }
}
