package ru.SpringDemo.SpringBot.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;

@Entity(name = "usersDataTable")
@Data
public class User {
    @Id
    private Long chatId;

    private String firstName;

    private String lastName;
    private String userName;

    private Timestamp registeredAt;

}
