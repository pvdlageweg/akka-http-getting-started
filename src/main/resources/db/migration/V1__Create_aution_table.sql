create table auctions (
  auction_id  int not null,
  description varchar(255) not null,
  primary key(auction_id)
);

insert into auctions(auction_id, description) values(123, 'test auction');