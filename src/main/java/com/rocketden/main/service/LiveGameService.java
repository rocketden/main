package com.rocketden.main.service;

import com.rocketden.main.dao.RoomRepository;
import org.springframework.stereotype.Service;

/**
 * Class to handle code updates and miscellaneous requests.
 */
@Service
public abstract class LiveGameService extends GameManagementService {

    protected LiveGameService(RoomRepository repository, SocketService socketService) {
        super(repository, socketService);
    }

}
