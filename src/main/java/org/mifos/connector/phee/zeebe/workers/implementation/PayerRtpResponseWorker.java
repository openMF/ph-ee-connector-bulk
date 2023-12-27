package org.mifos.connector.phee.zeebe.workers.implementation;

import org.mifos.connector.phee.zeebe.workers.BaseWorker;
import org.mifos.connector.phee.zeebe.workers.Worker;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PayerRtpResponseWorker extends BaseWorker {
    @Override
    public void setup() {

        newWorker(Worker.PAYER_RTP_RESPONSE, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();

        });
    }
}
