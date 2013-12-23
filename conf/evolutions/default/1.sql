# --- First database schema

# --- !Ups

create table person (
  id                   long not null primary key AUTO_INCREMENT,
  latitude             DOUBLE not null,
  longitude			   DOUBLE not null
);

insert into person(latitude, longitude) values(5.22, 54.21);
insert into person(latitude, longitude) values(5.21, 54.23);
insert into person(latitude, longitude) values(5.21, 54.22);
insert into person(latitude, longitude) values(5.24, 54.26);

# --- !Downs

drop table if exists user;
