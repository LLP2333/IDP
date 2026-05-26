package com.qvqw.idp.storage.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageReferenceChecker;
import com.qvqw.idp.storage.StorageType;
import com.qvqw.idp.storage.model.req.StorageCreateReq;
import com.qvqw.idp.storage.model.req.StorageStatusUpdateReq;
import com.qvqw.idp.storage.model.req.StorageUpdateReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StorageServiceImplTest {

    @Mock
    StorageRepository repository;

    @Mock
    ApplicationEventPublisher publisher;

    @Mock
    StorageReferenceChecker referenceChecker;

    StorageSecretCipher cipher = new StorageSecretCipher("unit-test-cipher");

    StorageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StorageServiceImpl(repository, cipher, publisher, referenceChecker);
    }

    private StorageCreateReq baseLocalReq() {
        StorageCreateReq req = new StorageCreateReq();
        req.setName("本地");
        req.setCode("local-1");
        req.setType(StorageType.LOCAL.getValue());
        req.setBucketName("/tmp/idp-files");
        req.setDomain("http://localhost:8080/file/local/");
        req.setRecycleBinEnabled(false);
        req.setSort(10);
        req.setStatus(1);
        return req;
    }

    private StorageCreateReq baseS3Req() {
        StorageCreateReq req = new StorageCreateReq();
        req.setName("MinIO");
        req.setCode("minio-1");
        req.setType(StorageType.S3.getValue());
        req.setAccessKey("ak");
        req.setSecretKey("sk");
        req.setEndpoint("http://localhost:9000");
        req.setBucketName("idp");
        req.setDomain("http://localhost:9000/idp/");
        req.setRecycleBinEnabled(true);
        req.setRecycleBinPath(".RECYCLE.BIN/");
        req.setSort(20);
        req.setStatus(1);
        return req;
    }

    @Test
    void createLocalStorageShouldSaveAndPublishEvent() {
        when(repository.existsByCode("local-1")).thenReturn(false);
        when(repository.save(any(Storage.class))).thenAnswer(inv -> {
            Storage s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        Long id = service.create(baseLocalReq());
        assertThat(id).isEqualTo(1L);
        verify(publisher).publishEvent(any(StorageServiceImpl.StorageChangedEvent.class));
    }

    @Test
    void createS3StorageShouldEncryptSecretKey() {
        when(repository.existsByCode("minio-1")).thenReturn(false);
        ArgumentCaptor<Storage> captor = ArgumentCaptor.forClass(Storage.class);
        when(repository.save(any(Storage.class))).thenAnswer(inv -> {
            Storage s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });

        service.create(baseS3Req());

        verify(repository).save(captor.capture());
        Storage saved = captor.getValue();
        assertThat(saved.getSecretKey()).isNotBlank().isNotEqualTo("sk");
        assertThat(cipher.decrypt(saved.getSecretKey())).isEqualTo("sk");
    }

    @Test
    void duplicateCodeShouldFail() {
        when(repository.existsByCode("local-1")).thenReturn(true);
        assertThatThrownBy(() -> service.create(baseLocalReq()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("编码已存在");
    }

    @Test
    void s3RequiresAccessKeyAndEndpointAndSecret() {
        StorageCreateReq req = baseS3Req();
        req.setAccessKey(null);
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access Key");
    }

    @Test
    void updateShouldKeepSecretKeyWhenBlank() {
        Storage existing = new Storage();
        existing.setId(3L);
        existing.setType(StorageType.S3.getValue());
        existing.setSecretKey(cipher.encrypt("original-sk"));
        existing.setName("old");
        existing.setBucketName("b");
        existing.setEndpoint("http://ep");
        existing.setAccessKey("ak");
        existing.setStatus(1);
        when(repository.findById(3L)).thenReturn(Optional.of(existing));

        StorageUpdateReq update = new StorageUpdateReq();
        update.setName("new");
        update.setAccessKey("ak2");
        update.setEndpoint("http://ep2");
        update.setBucketName("b2");
        update.setSecretKey(""); // 空字符串：不修改
        update.setSort(99);
        update.setStatus(1);

        ArgumentCaptor<Storage> captor = ArgumentCaptor.forClass(Storage.class);
        when(repository.save(any(Storage.class))).thenAnswer(inv -> inv.getArgument(0));
        service.update(3L, update);

        verify(repository).save(captor.capture());
        Storage saved = captor.getValue();
        assertThat(cipher.decrypt(saved.getSecretKey())).isEqualTo("original-sk");
        assertThat(saved.getName()).isEqualTo("new");
    }

    @Test
    void cannotDisableDefaultStorage() {
        Storage def = new Storage();
        def.setId(5L);
        def.setIsDefault(true);
        def.setStatus(1);
        when(repository.findById(5L)).thenReturn(Optional.of(def));

        StorageStatusUpdateReq req = new StorageStatusUpdateReq();
        req.setStatus(2);

        assertThatThrownBy(() -> service.updateStatus(5L, req))
                .hasMessageContaining("默认存储不能禁用");
    }

    @Test
    void cannotDeleteDefaultStorage() {
        Storage def = new Storage();
        def.setId(6L);
        def.setName("default");
        def.setIsDefault(true);
        when(repository.findAllById(List.of(6L))).thenReturn(List.of(def));

        assertThatThrownBy(() -> service.delete(List.of(6L)))
                .hasMessageContaining("默认存储不能删除");
    }

    @Test
    void deleteFailsWhenReferenced() {
        Storage s = new Storage();
        s.setId(7L);
        when(repository.findAllById(List.of(7L))).thenReturn(List.of(s));
        when(referenceChecker.countFilesByStorageIds(List.of(7L))).thenReturn(3L);
        assertThatThrownBy(() -> service.delete(List.of(7L)))
                .hasMessageContaining("尚有文件");
    }

    @Test
    void setDefaultClearsPrevious() {
        Storage prev = new Storage();
        prev.setId(8L);
        prev.setIsDefault(true);
        prev.setStatus(1);
        Storage target = new Storage();
        target.setId(9L);
        target.setIsDefault(false);
        target.setStatus(1);

        when(repository.findById(9L)).thenReturn(Optional.of(target));
        when(repository.findFirstByIsDefaultTrue()).thenReturn(Optional.of(prev));

        service.setDefault(9L);

        verify(repository).save(prev);
        verify(repository).save(target);
        assertThat(prev.getIsDefault()).isFalse();
        assertThat(target.getIsDefault()).isTrue();
    }

    @Test
    void listShouldUseSpecificationAndSort() {
        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        service.list(null);
        verify(repository).findAll(any(Specification.class), any(Sort.class));
    }
}
