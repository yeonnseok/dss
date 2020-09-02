package io.github.ztkmkoo.dss.server.tcp;

import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

class DssTcpServerTest {

    private static final String SSL_PASSWORD = "dss123";

    private static void startOnNewThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    private static void stopDssTcpServerAfterActivated(DssTcpServer dssTcpServer, int waitStartupSeconds, int waitShutdownSeconds) {
        startOnNewThread(() -> {
            await()
                .atMost(waitStartupSeconds, TimeUnit.SECONDS)
                .until(dssTcpServer::isActivated);
            dssTcpServer.stop();

            await()
                .atMost(waitShutdownSeconds, TimeUnit.SECONDS)
                .until(dssTcpServer::isShutdown);
        });
    }

    private static PrivateKey loadPrivateKeyFromFile(File file) throws
        IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        final FileInputStream fis = new FileInputStream(file);
        final byte[] buffer = new byte[fis.available()];
        fis.read(buffer);
        fis.close();

        final KeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private static X509Certificate loadX509CertificateFromFile(File file) throws IOException, CertificateException {
        final FileInputStream fis = new FileInputStream(file);
        final byte[] buffer = new byte[fis.available()];
        fis.read(buffer);
        fis.close();

        final ByteArrayInputStream bais = new ByteArrayInputStream(buffer);

        final CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate)factory.generateCertificate(bais);
    }

    private static File loadFromTestResources(String path) throws UnsupportedEncodingException {
        final ClassLoader classLoader = DssTcpServerTest.class.getClassLoader();
        return new File(URLDecoder.decode(classLoader.getResource(path).getFile(),"UTF-8"));
    }

    @Test
    void start() throws InterruptedException {

        final DssTcpServer dssTcpServer = new DssTcpServer("127.0.0.1", 8181);

        stopDssTcpServerAfterActivated(dssTcpServer, 10, 15);

        dssTcpServer.start();

        assertTrue(dssTcpServer.isShutdown());
    }

    @Test
    void startSsl() throws Exception {
        final PrivateKey privateKey = loadPrivateKeyFromFile(loadFromTestResources("ssl/private.der"));
        final X509Certificate certificate = loadX509CertificateFromFile(loadFromTestResources("ssl/private.crt"));

        final SslContext sslContext = SslContextBuilder.forServer(privateKey, SSL_PASSWORD, certificate).build();

        final DssTcpServer dssTcpServer = new DssTcpServer("127.0.0.1", 8181, true, sslContext);
        stopDssTcpServerAfterActivated(dssTcpServer, 10 ,15);

        dssTcpServer.start();

        assertTrue(dssTcpServer.isShutdown());
    }
}