# --- First database schema

# --- !Ups

create table person (
  id                   long not null primary key AUTO_INCREMENT,
  latitude             DOUBLE not null,
  longitude			   DOUBLE not null,
  token			   	   varchar(255) not null
);

insert into person(latitude, longitude, token) values(5.22, 54.21, 'string1');
insert into person(latitude, longitude, token) values(5.21, 54.23, 'string1');
insert into person(latitude, longitude, token) values(5.21, 54.22, 'string1');
insert into person(latitude, longitude, token) values(5.24, 54.26, 'string1');

# --- !Downs

drop table if exists user;
