create table auctions (
  auction_id  int not null,
  description varchar(255) not null,
  primary key(auction_id)
);

insert into auctions(auction_id, description) values(123, 'test auction');

create table bids (
  bid_id  int not null,
  auction_id  int not null,
  offer double not null,
  primary key(bid_id)
);

insert into bids(bid_id, auction_id, offer) values(2, 123, 12.34);
insert into bids(bid_id, auction_id, offer) values(3, 123, 17.00);

