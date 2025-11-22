package com.traker.traker.security;

import com.traker.traker.entity.User;
import com.traker.traker.repository.UserRepository;
import com.traker.traker.security.crypto.DataEncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * Manages per-user encryption keys. Each user receives a random 256-bit key
 * that is stored encrypted with a server-side master key.
 */
@Service
public class UserEncryptionService {

    private final UserRepository userRepository;
    private final DataEncryptionService dataEncryptionService;

    private final byte[] masterKey;

    public UserEncryptionService(UserRepository userRepository,
                                 DataEncryptionService dataEncryptionService,
                                 @Value("${app.security.master-key}") String masterKeyValue) {
        this.userRepository = userRepository;
        this.dataEncryptionService = dataEncryptionService;
        this.masterKey = dataEncryptionService.decodeKey(masterKeyValue);
    }

    public void assignFreshKey(User user) {
        byte[] rawKey = dataEncryptionService.generateKey();
        user.setEncryptedDataKey(dataEncryptionService.encryptBytes(masterKey, rawKey));
        user.setDecryptedDataKey(Arrays.copyOf(rawKey, rawKey.length));
    }

    @Transactional
    public User ensureUserKey(User user) {
        if (user.getEncryptedDataKey() == null) {
            assignFreshKey(user);
            userRepository.save(user);
            return user;
        }
        if (user.getDecryptedDataKey() == null) {
            byte[] rawKey = dataEncryptionService.decryptToBytes(masterKey, user.getEncryptedDataKey());
            user.setDecryptedDataKey(Arrays.copyOf(rawKey, rawKey.length));
        }
        return user;
    }
}
