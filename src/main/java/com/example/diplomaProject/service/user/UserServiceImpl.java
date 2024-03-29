package com.example.diplomaProject.service.user;

import com.example.diplomaProject.domain.constant.Code;
import com.example.diplomaProject.domain.dto.UserDto;
import com.example.diplomaProject.domain.entity.Role;
import com.example.diplomaProject.domain.entity.User;
import com.example.diplomaProject.domain.mapper.user.UserListMapper;
import com.example.diplomaProject.domain.mapper.user.UserMapper;
import com.example.diplomaProject.domain.response.Response;
import com.example.diplomaProject.domain.response.SuccessResponse;
import com.example.diplomaProject.domain.response.error.Error;
import com.example.diplomaProject.domain.response.error.ErrorResponse;
import com.example.diplomaProject.repository.RoleRepository;
import com.example.diplomaProject.repository.UserRepository;
import com.example.diplomaProject.util.EncryptPassword;
import com.example.diplomaProject.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ValidationUtils validation;
//    private final EncryptPassword encryptPassword;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    private final UserListMapper userListMapper;
    private final UserMapper userMapper;


    public ResponseEntity<Response> getAll() {

        List<User> users = userRepository.findAllByOrderBySecondName();
        List<UserDto> dtoUsers = userListMapper.toDtoList(users);

        return new ResponseEntity<>(SuccessResponse.builder().data(dtoUsers).build(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Response> getById(Long id) {

        Optional<User> userOptional = userRepository.findById(id);
        if(userOptional.isPresent()) {
            UserDto userDto = userMapper.toDto(userOptional.get());
            return new ResponseEntity<>(SuccessResponse.builder().data(userDto).build(), HttpStatus.OK);
        } else {
            log.error("user with id: {} is not found", id);
            return new ResponseEntity<>(ErrorResponse.builder()
                    .error(Error.builder().code(Code.NOT_FOUND).message("Пользователь не найден").build())
                    .build(), HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<Response> add(UserDto userDto) {

        validation.validationRequest(userDto);
        User user = userMapper.toEntity(userDto);

//        user.setPassword(encryptPassword.encryptPassword(user.getPassword()));
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        Set<Role> rolesCopy = new HashSet<>(user.getRoles());
        for(Role role : rolesCopy) {
            Optional<Role> optionalRole = roleRepository.findRoleByTitle(role.getTitle());
            if(optionalRole.isPresent()) {
                user.getRoles().remove(role);
                user.getRoles().add(optionalRole.get());
            }
        }
        userRepository.save(user);

        return new ResponseEntity<>(SuccessResponse.builder().build(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Response> update(UserDto userDto) {

        validation.validationRequest(userDto);
        User user = userMapper.toEntity(userDto);

        Optional<User> userOptional = userRepository.findById(user.getId());
        if(userOptional.isPresent()) {
            User baseUser = userOptional.get();
            baseUser.setFirstName(user.getFirstName());
            baseUser.setSecondName(user.getSecondName());
//            baseUser.setPassword(user.getPassword());
            baseUser.setRoles(user.getRoles());
            Set<Role> rolesCopy = new HashSet<>(user.getRoles());
            for(Role role : rolesCopy) {
                Optional<Role> optionalRole = roleRepository.findRoleByTitle(role.getTitle());
                if(optionalRole.isPresent()) {
                    user.getRoles().remove(role);
                    user.getRoles().add(optionalRole.get());
                }
            }
            userRepository.save(baseUser);

            return new ResponseEntity<>(SuccessResponse.builder().build(), HttpStatus.OK);
        }

        return new ResponseEntity<>(ErrorResponse.builder()
                .error(Error.builder().code(Code.INVALID_VALUE).message("Пользователь не найден").build())
                .build(), HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<Response> remove(Long id) {

        if(!userRepository.existsById(id)) {
            log.error("user with id: {} is not found", id);
            return new ResponseEntity<>(ErrorResponse.builder()
                    .error(Error.builder().code(Code.NOT_FOUND).message("Пользователь не найден").build())
                    .build(), HttpStatus.BAD_REQUEST);
        }
        userRepository.deleteById(id);

        return new ResponseEntity<>(SuccessResponse.builder().build(), HttpStatus.OK);
    }

//    @Override
//    public ResponseEntity<Response> getByFullName(String fullName) {
//
//        UserDto user = (UserDto) loadUserByUsername(fullName);
//        return new ResponseEntity<>(SuccessResponse.builder().data(user).build(), HttpStatus.OK);
//    }


//    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        String [] secondAndFirstName = username.split(" ");
//        if(secondAndFirstName.length != 2) {
//            log.error("user with name `{}` not found", username);
//            throw new UsernameNotFoundException("user with name `" + username + "` not found");
//        }
//        String secondName = secondAndFirstName[0];
//        String firstName = secondAndFirstName[1];
//        User user = userRepository.findBySecondNameAndFirstName(secondName, firstName)
//                .orElseThrow(() ->
//                    new UsernameNotFoundException("user with name `" + secondName + " " + firstName + "` not found")
//                );
//        return userMapper.toDto(user);
//    }
}
