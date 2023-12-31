package com.example.diplomaProject.service.connectedDB;

import com.example.diplomaProject.domain.constant.Code;
import com.example.diplomaProject.domain.dto.ConnDbDto;
import com.example.diplomaProject.domain.entity.ConnDb;
import com.example.diplomaProject.domain.mapper.connDb.ConnDbMapper;
import com.example.diplomaProject.domain.response.Response;
import com.example.diplomaProject.domain.response.SuccessResponse;
import com.example.diplomaProject.domain.response.error.Error;
import com.example.diplomaProject.domain.response.error.ErrorResponse;
import com.example.diplomaProject.repository.ConnectedDbRepository;
import com.example.diplomaProject.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectedDbServiceImpl implements ConnectedDbService {

    private final ConnectedDbRepository dbRepository;
    private final ValidationUtils validationUtils;

    private final ConnDbMapper connDbMapper;

    @Override
    public ResponseEntity<Response> add(ConnDbDto connectedDb) {

        validationUtils.validationRequest(connectedDb);

        dbRepository.save(connDbMapper.toEntity(connectedDb));
        return new ResponseEntity<>(SuccessResponse.builder().build(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Response> getAll() {

        List<ConnDb> connDbList = dbRepository.findAll();
        return new ResponseEntity<>(SuccessResponse.builder().data(connDbList).build(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Response> getById(Long id) {

        if(id == null) {
            log.error("id is null");
            return new ResponseEntity<>(ErrorResponse.builder().error(Error.builder()
                    .code(Code.INVALID_VALUE).message("null id")
                    .build()).build(), HttpStatus.BAD_REQUEST);
        }
        Optional<ConnDb> connDb = dbRepository.findById(id);
        if(connDb.isPresent()) {
            return new ResponseEntity<>(SuccessResponse.builder().data(connDb.get()).build(), HttpStatus.OK);
        } else {
            log.error("connected db with id `{}` is not found", id);
            return new ResponseEntity<>(ErrorResponse.builder().error(Error.builder()
                            .code(Code.NOT_FOUND).message("База с таким id не найдена")
                    .build()).build(), HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<Response> update(ConnDbDto connectedDb) {

        validationUtils.validationRequest(connectedDb);

        ConnDb origConnDb = dbRepository.findById(connectedDb.getId()).get();
        ConnDb fixedConnDb = connDbMapper.toEntity(connectedDb);
        origConnDb.setTitle(fixedConnDb.getTitle());
        origConnDb.setUrl(fixedConnDb.getUrl());
        origConnDb.setUsername(fixedConnDb.getUsername());
        origConnDb.setPassword(fixedConnDb.getPassword());
        origConnDb.setDbType(fixedConnDb.getDbType());
        dbRepository.save(origConnDb);

        return new ResponseEntity<>(SuccessResponse.builder().build(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Response> remove(Long id) {

        if(id == null) {
            log.error("id is null");
            return new ResponseEntity<>(ErrorResponse.builder().error(Error.builder()
                    .code(Code.INVALID_VALUE).message("null id")
                    .build()).build(), HttpStatus.BAD_REQUEST);
        }
        Optional<ConnDb> connDb = dbRepository.findById(id);
        if(connDb.isPresent()) {
            dbRepository.deleteById(id);
            return new ResponseEntity<>(SuccessResponse.builder().build(), HttpStatus.OK);
        } else {
            log.error("connected db with id `{}` is not found", id);
            return new ResponseEntity<>(ErrorResponse.builder().error(Error.builder()
                    .code(Code.NOT_FOUND).message("База с таким id не найдена")
                    .build()).build(), HttpStatus.NOT_FOUND);
        }
    }

    private ResponseEntity<Response> switchDb(Long id) {
//
//        ConnectedDataBase dataBase = dbRepository.findById(id).get();
//
//        Configuration config = new Configuration();
//        config.setProperty("hibernate.connection.driver_class",
//                switch (dataBase.getDbType()) {
//                    case MYSQL -> "com.mysql.jdbc.Driver";
//                    case POSTGRES -> "org.postgresql.Driver";
//                    case ORACLE -> "";
//                } );
//        config.setProperty("hibernate.connection.url", dataBase.getUrl());
//        config.setProperty("hibernate.connection.username", dataBase.getPassword());

        return null;
    }

}
