package io.quarkus.amazon.secretsmanager.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkus.amazon.common.deployment.AmazonClientAsyncResultBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientAsyncTransportBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientInterceptorsPathBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientSyncResultBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientSyncTransportBuildItem;
import io.quarkus.amazon.common.deployment.AmazonHttpClients;
import io.quarkus.amazon.common.deployment.RequireAmazonClientBuildItem;
import io.quarkus.amazon.common.runtime.AmazonClientApacheTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientCommonRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkus.amazon.secretsmanager.runtime.SecretsManagerBuildTimeConfig;
import io.quarkus.amazon.secretsmanager.runtime.SecretsManagerClientProducer;
import io.quarkus.amazon.secretsmanager.runtime.SecretsManagerRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClientBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

public class SecretsManagerProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_SECRETS_MANAGER = "amazon-secretsmanager";

    SecretsManagerBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_SECRETS_MANAGER;
    }

    @Override
    protected String configName() {
        return "secretsmanager";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(SecretsManagerClient.class.getName());
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(SecretsManagerAsyncClient.class.getName());
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/secretsmanager/execution.interceptors";
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.unremovableOf(SecretsManagerClientProducer.class);
    }

    @BuildStep
    void setup(
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors) {

        setupExtension(extensionSslNativeSupport, feature, interceptors);
    }

    @BuildStep
    void discover(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        discoverClient(beanRegistrationPhase, requireClientProducer);
    }

    @BuildStep
    void setupClient(List<RequireAmazonClientBuildItem> clientRequirements,
            BuildProducer<AmazonClientBuildItem> clientProducer) {

        setupClient(clientRequirements, clientProducer, buildTimeConfig.sdk, buildTimeConfig.syncClient);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonApacheHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupApacheSyncTransport(List<AmazonClientBuildItem> amazonClients, SecretsManagerRecorder recorder,
            AmazonClientApacheTransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createApacheSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient,
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonUrlConnectionHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupUrlConnectionSyncTransport(List<AmazonClientBuildItem> amazonClients, SecretsManagerRecorder recorder,
            AmazonClientUrlConnectionTransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createUrlConnectionSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient,
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonNettyHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupNettyAsyncTransport(List<AmazonClientBuildItem> amazonClients, SecretsManagerRecorder recorder,
            AmazonClientNettyTransportRecorder transportRecorder,
            BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports) {

        createNettyAsyncTransportBuilder(amazonClients,
                transportRecorder,
                recorder.getAsyncConfig(),
                asyncTransports);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createClientBuilders(SecretsManagerRecorder recorder,
            AmazonClientCommonRecorder commonRecorder,
            List<AmazonClientSyncTransportBuildItem> syncTransports,
            List<AmazonClientAsyncTransportBuildItem> asyncTransports,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<AmazonClientSyncResultBuildItem> clientSync,
            BuildProducer<AmazonClientAsyncResultBuildItem> clientAsync,
            ExecutorBuildItem executorBuildItem) {

        createClientBuilders(recorder,
                commonRecorder,
                buildTimeConfig.sdk,
                syncTransports,
                asyncTransports,
                SecretsManagerClientBuilder.class,
                SecretsManagerAsyncClientBuilder.class,
                null,
                syntheticBeans,
                clientSync,
                clientAsync,
                executorBuildItem);
    }
}
