package com.xinktech.k8s;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class Main {

    /*kubernetes*/
    private final static String MASTER_URL = "https://192.168.16.190:6443/";
    public static void main(String[] args){
        Logger log = LoggerFactory.getLogger(Main.class);
        Config config = new ConfigBuilder()
                .withMasterUrl(MASTER_URL)
                .build();
       String caPath = ClassLoader.getSystemResource("public/ca.crt").getPath();
       config.setCaCertFile(caPath);
       config.setClientCertFile( ClassLoader.getSystemResource("public/apiserver-kubelet-client.crt").getPath());
       config.setClientKeyFile( ClassLoader.getSystemResource("public/apiserver-kubelet-client.key").getPath());

        try (KubernetesClient client = new AutoAdaptableKubernetesClient(config)) {
            //Namespace ns = new NamespaceBuilder().withNewMetadata().withName("").endMetadata().build();

            ContainerPort cp1 = new ContainerPortBuilder()
                    .withContainerPort(5555)
                    .withProtocol("TCP")
                    .build();

            EnvVar envVar1 = new EnvVarBuilder()
                    .withName("DB_HOST")
                    .withValue("192.168.16.189")
                    .build();
            EnvVar envVar2 = new EnvVarBuilder()
                    .withName("DB_PORT")
                    .withValue("3306")
                    .build();
            EnvVar envVar3 = new EnvVarBuilder()
                    .withName("DB_USER")
                    .withValue("root")
                    .build();
            EnvVar envVar4 = new EnvVarBuilder()
                    .withName("DB_PASSWD")
                    .withValue("123456")
                    .build();
            EnvVar envVar5 = new EnvVarBuilder()
                    .withName("DB_NAME")
                    .withValue("AEServerDB")
                    .build();
            EnvVar envVar6 = new EnvVarBuilder()
                    .withName("DOWNLOAD_SERVER")
                    .withValue("root@192.168.16.189:/root/nginx/www/model_files/")
                    .build();
            EnvVar envVar7 = new EnvVarBuilder()
                    .withName("UI_SERVER_API")
                    .withValue("localhost:8090")
                    .build();
            EnvVar envVar8 = new EnvVarBuilder()
                    .withName("UI_SERVER_USERNAME")
                    .withValue("admin")
                    .build();
            EnvVar envVar9 = new EnvVarBuilder()
                    .withName("UI_SERVER_PASSWORD")
                    .withValue("admin")
                    .build();
            EnvVar envVar10 = new EnvVarBuilder()
                    .withName("MODEL_KEY")
                    .withValue("L2hvbWUvZGVlcHRoaW5rZXIvYWUtc2VydmVyL2FlL2JhY2tlbmRkZXYvdXNlcnMvYWRtaW4vam9icy9qb2I1OTc=")
                    .build();
            List<EnvVar> envVars = new ArrayList<>();
            Collections.addAll(envVars, envVar1, envVar2, envVar3, envVar4, envVar5, envVar6, envVar7, envVar8, envVar9, envVar10);
            Container container = new ContainerBuilder()
                    .withName("ae-server")
                    .withImage("hub.nat.xinktech.com/yscz/ae-server:latest")
                    .addToPorts(cp1)
                    .addAllToEnv(envVars)
                    .build();

            Map<String, String>  nodeS=new HashMap<String,String>();
            nodeS.put("gup-node", "gup-02");
            Deployment deployment = new DeploymentBuilder()
                    .withApiVersion("extensions/v1beta1")
                    .withKind("Deployment")
                    .withNewMetadata()
                    .withName("deepthink-api")
                    .withNamespace("gpu-ns")
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(1)
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("task", "api")
                    .addToLabels("dpthink-api", "dpt-api")
                    .endMetadata()
                    .withNewSpec()
                    .withNodeSelector(nodeS)
                    .addNewImagePullSecret()
                    .withName("yscz-registrykey")
                    .endImagePullSecret()
                    .addToContainers(container)
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

           deployment = client.extensions().deployments().inNamespace("gpu-ns").withName("deepthink-api").create(deployment);

         Map<String,Object> oj=  deployment.getStatus().getAdditionalProperties();

         // client.services().inNamespace("gpu-ns").withName("deepthink-api-sv").get().getStatus().getLoadBalancer().getIngress().get(0).getIp();

         String podIP= client.pods().inNamespace("gpu-ns").withName("deepthink-api").get().getStatus().getHostIP() ; //Get pod HostIP

        String podsIP=  client.nodes().withName("gpu-02").get().getStatus().getAddresses().iterator().next().getAddress(); // Get a node address by node status


             log.info("deployment", deployment);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
