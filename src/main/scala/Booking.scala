import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import java.util.concurrent.ThreadLocalRandom
import java.util.{Calendar, Date, UUID}
import scala.collection.mutable.ListBuffer
import java.time.LocalDate

/**
 * any case object or case class with a parameter sender_ is one which can be  implemented without it
 * I was having issues with some actors unexpectedly terminating due to deadletters. Meaning after
 * they finish processing, they return to find no actor address to receive the message. So as a hack,
 * i propagated the address. I should fix this before submission
 */

trait Kind
case object Hotel extends Kind
case object Apartment extends Kind
case object Resort extends Kind
case object None extends Kind

trait Search
case class SearchHotel(date: LocalDate) extends Search
case class SearchApartment(date: LocalDate) extends Search
case class SearchResort(date: LocalDate) extends Search

//trait Reserve{
//  val client: Client
//  val date: LocalDate
//}
case class Reservation(propertyId: UUID, client: Client, date: LocalDate)
case class MakeReservation(propertyId: UUID, client:Client, date: LocalDate)
case class CancelReservation(propertyId: UUID, client:Client, date: LocalDate)
//case class AddReservation(propertyId: UUID, client: Client, date: LocalDate)
case class RemoveReservation(sender_ : ActorRef)
case class AddReservation(sender_ : ActorRef)

case class SendReservation(sender_ : ActorRef)

case class PropertyReserved(msg: String, sender_ : ActorRef)
case class ChooseDifferentProperty(msg: String)

//case object PropertyBooked
//case object PropertyNotBooked
case class  Reserved(propertyId: UUID, client: Client, date: LocalDate)
case class NotReserved(msg: String, sender_ : ActorRef)

case class ReservationCornfirmed(msg: String, propertyId: UUID, date: LocalDate )
case object ReservationFailed
case class ReservationCancelled(client: Client, propertyId: UUID, date: LocalDate, sender_ : ActorRef)

case class CancellationFailed(clientName: String, propertyId: UUID, date: LocalDate)
case class CancellationSuccesful(clientName: String, propertyId: UUID, date: LocalDate)

case class Property(id: UUID, name: String, propertyType: Kind, category: Int, location: Locality, unavailability: ListBuffer[LocalDate])
case class Locality(country: String, city: String)
case class Client(name: String, age: Int, passportNumber: String)
case object PropertyNotAvailable
case class SearchResults(properties: ListBuffer[Property])
case class Slug(cancellation: Boolean, propertyId: UUID)

object RandomValues {
  // localities
  var localities = ListBuffer(
    Locality("Belgium", "Brussels"),
    Locality("Belgium","Charleroi"),
    Locality("Belgium","Antwerpen"),
    Locality("Belgium", "Mons"),
    Locality("United Kingdom","Belfast"),
    Locality("United Kingdom","Manchester"),
    Locality("United States","Boston"),
    Locality("United States","Houston")
  )
  // properties
  /**
   * I have added UUID's for testing cancellation of reservation
   */
  var properties = ListBuffer(
    Property(UUID.fromString("fbd23f3f-3323-423f-8797-4d15c38f5cbf"),"Juliana Hotel", Hotel , 5, localities(0), ListBuffer(LocalDate.of(2022,7,24))),
    Property(UUID.fromString("20deda85-9020-4153-a5c1-8839f5c862bd"),"The Scott", Hotel , 4, localities(0), ListBuffer(LocalDate.of(2022,7,20))),
    Property(UUID.randomUUID(),"Pillows City Hotel", Hotel , 5, localities(0), ListBuffer()),
    Property(UUID.randomUUID(),"Hotel Central", Hotel , 3, localities(2), ListBuffer()),
    Property(UUID.randomUUID(),"Hotel Central", Hotel , 4, localities(2), ListBuffer()),
    Property(UUID.randomUUID(),"Boutique Hotel", Hotel , 4, localities(1), ListBuffer()),
    Property(UUID.fromString("76a1ef31-adc1-4594-924f-0ecff1133640"),"Smartflats Premium - Palace du Grand Sablon", Apartment , 5, localities(0), ListBuffer()),
    Property(UUID.randomUUID(),"Smartflats Premium - Palace du Grand Sablon", Apartment , 5, localities(0), ListBuffer()),
    Property(UUID.randomUUID(),"International Students Complex - Old Town", Apartment , 4, localities(2), ListBuffer()),
    Property(UUID.randomUUID(),"Smartflats Diamant", Apartment , 4, localities(2), ListBuffer()),
    Property(UUID.randomUUID(),"U-Residence", Apartment , 3, localities(3), ListBuffer()),
    Property(UUID.randomUUID(),"Center Parcs Les Ardennes", Resort , 5, localities(0), ListBuffer()),
    Property(UUID.randomUUID(),"Center Parcs Les Ardennes", Resort , 5, localities(0), ListBuffer()),
    Property(UUID.randomUUID(),"Village Sunparks Kempense Meren", Resort , 3, localities(1), ListBuffer()),
    Property(UUID.randomUUID(),"Castle of eglantines", Resort , 5, localities(3), ListBuffer()),
    Property(UUID.randomUUID(),"Castle of eglantines", Resort , 4, localities(3), ListBuffer())
  )
  var clients = ListBuffer(
    Client("Linoel Bettington", 25, "KJ9658716"),
    Client("Huberto Galpen", 32, "KJ9658795"),
    Client("Way Berard", 29, "KJ8658716"),
    Client("Louis Matson", 26, "KJ9858036"),
    Client("Janet Betchley", 39, "KJ1658549"),
    Client("Vince Rosenfelt", 46, "KJ7758748"),
    Client("Glenna Celloni", 24, "KJ8648763"),
    Client("Roxie Hardbattle", 41, "KJ3658514"),
    Client("Eleanore Cohane", 35, "KJ9658619"),
    Client("Kizzie Hillum", 51, "KJ6548304"),
    Client("Jareb Mullaly", 32, "KJ7669714")
  )
  /**
   * I have added two Reservations in both bookings and reservations for testing cancellation.
   */
  //  var reservations = ListBuffer[Reservation]()
  var bookings: ListBuffer[Reservation] = ListBuffer(
    Reservation(UUID.fromString("fbd23f3f-3323-423f-8797-4d15c38f5cbf"),clients(0),LocalDate.of(2022,7,24)),
    Reservation(UUID.fromString("20deda85-9020-4153-a5c1-8839f5c862bd"),clients(4),LocalDate.of(2022,7,20))
  )
  //  var bookings = ListBuffer[Reservation]()
  var reservations: ListBuffer[Reservation] = ListBuffer(
    Reservation(UUID.fromString("fbd23f3f-3323-423f-8797-4d15c38f5cbf"),clients(0),LocalDate.of(2022,7,24)),
    Reservation(UUID.fromString("20deda85-9020-4153-a5c1-8839f5c862bd"),clients(4),LocalDate.of(2022,7,20))
  )

}
class ReservationService(propertyId: UUID, client: Client, date: LocalDate) extends Actor with ActorLogging{

  def receive: Receive = {
    case AddReservation(sender_) =>
      if(isReservationAvailable(propertyId, client, date)){
        //@ams -> This date might have already been reserved by someone from a different booking system
      RandomValues.reservations += Reservation(propertyId, client, date)
      sender() ! PropertyReserved(s"A reservation has been successfully made for ${client.name}", sender_)
      }else {
        sender() ! NotReserved(s"A reservation cannot be made for ${client.name} on ${date}.", sender_)
      }
    case RemoveReservation(sender_) =>
      RandomValues.reservations -= Reservation(propertyId, client, date)
      sender() ! ReservationCancelled(client, propertyId, date, sender_)
  }
  def isReservationAvailable(propertyId: UUID, client: Client, date: LocalDate): Boolean = {
    /**
     * Bad programming. iterating reservations and
     * updating it above at the same time. cuncurrent propblems might arise @ams-> Fix
     */
    val reservation = RandomValues.reservations.filter(reservation => reservation.propertyId.equals(propertyId) &&
      reservation.date.equals(date))
    if(reservation.isEmpty) true
    else false
  }
}
class ChildService(client: Client, propertyId: UUID, date: LocalDate, parent_ : ActorRef, replyTo: ActorRef) extends Actor with ActorLogging {

  def receive: Receive = {
    case SendReservation(sender_) =>
      if(isPropertyAvailable(propertyId, client, date)){
        log.info(s"A reservation request has been made by ${client.name}.")
        val reservationService: ActorRef = context.actorOf(Props(new ReservationService(propertyId, client, date)))
        reservationService ! AddReservation(sender_)
      }else{
       val msg = s"Property cannot be reserved on ${date}. Please choose a different property or date."
        replyTo ! ChooseDifferentProperty(msg)
      }
    case PropertyReserved(msg, sender_) =>
      parent_ ! Reserved(propertyId, client, date) //the parent
//      replyTo ! ReservationCornfirmed(msg, propertyId, date)
      sender_ ! ReservationCornfirmed(msg, propertyId, date)
      context.stop(self)
    case NotReserved(msg, sender_) =>
      parent_ ! ReservationFailed
      sender_ ! NotReserved(msg, sender_)
      context.stop(self)
    case _ =>
  }
  def isPropertyAvailable(propertyId: UUID, client: Client, date: LocalDate): Boolean = {
    val booking = RandomValues.bookings.filter(booking => booking.propertyId.equals(propertyId) &&
      booking.date.equals(date))
    if(booking.isEmpty) true
    else false
  }
}
class SystemService extends Actor with ActorLogging {

  def receive: Receive = {
    case SearchHotel(date) =>
      val hotels = searchAvailableProperties(Hotel, date)
      if (hotels.isEmpty)
        sender() ! PropertyNotAvailable
      else
        sender() ! SearchResults(hotels)
    case SearchApartment(date) =>
      val apartments = searchAvailableProperties(Apartment, date)
      if (apartments.isEmpty)
        sender() ! PropertyNotAvailable
      else
        sender() ! SearchResults(apartments)
    case SearchResort(date) =>
      val resorts = searchAvailableProperties(Resort, date)
      if (resorts.isEmpty)
        sender() ! PropertyNotAvailable
      else
        sender() ! SearchResults(resorts)
    case MakeReservation(propertyId, client, date) =>
      val childService: ActorRef = context.actorOf(Props(new ChildService(client, propertyId, date , self, sender())))
      childService ! SendReservation(sender())
    case Reserved(propertyId, client, date) =>
      //update bookings and unavailability listBuffer of property
      RandomValues.bookings += Reservation(propertyId, client, date)
      RandomValues.properties.foreach(property =>
        if(property.id == propertyId)
          property.unavailability += date
      )
    case ReservationFailed =>
    case CancelReservation(propertyId, client, date) =>
      val bookedDate = RandomValues.bookings.filter( booking =>
        booking.propertyId.equals(propertyId) && booking.client.equals(client))
        .map( booking => booking.date).head
      if((bookedDate.getDayOfMonth - date.getDayOfMonth) >= 2){ // 48rs or more before bookedDate
        val reservationService: ActorRef = context.actorOf(Props(new ReservationService(propertyId, client, bookedDate)))
        reservationService ! RemoveReservation(sender())
      }else{
        sender() ! CancellationFailed(client.name, propertyId, bookedDate)
      }
    case ReservationCancelled(client, propertyId, date, sender_) =>
      //update bookings and unavailability listBuffer of property
      RandomValues.bookings -= Reservation(propertyId, client, date)
      RandomValues.properties.foreach(property =>
        if(property.id == propertyId)
        property.unavailability -= date
      )
      sender_ ! CancellationSuccesful(client.name, propertyId, date)
    case _ =>
  }

  def searchAvailableProperties(kind: Kind, date: LocalDate) = {
    RandomValues.properties.filter(property => property.propertyType == kind && !property.unavailability.contains(date))
  }
}
class ClientActor(client: Client,
                  propertyType: Kind,
                  reservationDate: LocalDate,
                  action : ListBuffer[Slug],
                  searchActor: ActorRef) extends Actor with ActorLogging{

if(action.isEmpty) {
  if(propertyType == Hotel){
    searchActor ! SearchHotel(reservationDate)
  }else if(propertyType == Apartment){
    searchActor ! SearchApartment(reservationDate)
  }else{ //Resort
    searchActor ! SearchResort(reservationDate)
  }
}else {
  if(action(0).cancellation){  // true
    log.info(s"${client.name} wishes to cancel a reservation at ${getPropertyName(action(0).propertyId)}.")
    searchActor ! CancelReservation(action(0).propertyId, client, reservationDate) // reservaionDate is date at this moment.
  }else{
    log.info(s"${client.name} wishes to reserve an accommodation at ${getPropertyName(action(0).propertyId)} on ${reservationDate}")
    searchActor ! MakeReservation(action(0).propertyId, client, reservationDate)
  }
}

  def rnd = ThreadLocalRandom.current

  def receive: Receive = {
    case PropertyNotAvailable => log.info(s"Property not available on ${reservationDate} ... Select a different date.")
    case SearchResults(msg) =>
      val propertyToReserve = msg(rnd.nextInt(msg.size))
      log.info(s"${client.name} wishes to reserve an accommodation at ${propertyToReserve.name} on ${reservationDate}")
      searchActor ! MakeReservation(propertyToReserve.id, client, reservationDate)
    case ReservationCornfirmed(msg, propertyId, date) =>
      log.info(s"${msg} to ${getPropertyName(propertyId)} on ${date}.")
    case NotReserved(msg, sender_) =>
      log.info(s"${msg} Please Select a different date or property.")
    case ChooseDifferentProperty(msg) =>
      log.info(msg)
    case CancellationSuccesful(clientName, propertyId, date) =>
      log.info(s"The reservation by ${clientName} for ${getPropertyName(propertyId)} on ${date} was successfully cancelled.")
    case CancellationFailed(clientName, propertyId, date) =>
      log.info(s"The reservation by ${clientName} for ${getPropertyName(propertyId)} on ${date} was not cancelled.")
      log.info(s"You can only cancel within 48 hours or more before the date of the reservation.")
    case _ =>
  }
  def getPropertyName(propertyId: UUID): String = {
    RandomValues.properties.filter(property => property.id.equals(propertyId))
      .map(property => property.name).head
  }
}

object Booking extends App {

  val system = ActorSystem("OnlineBooking")
  println(system)
  println("Welcome to Kijamii Booking")
  val searchActor = system.actorOf(Props[SystemService], "search")

  val Client1 = system.actorOf(Props(new ClientActor(RandomValues.clients(0), Hotel, LocalDate.now, ListBuffer(), searchActor)), "searchHotel")
  Thread.sleep(1000)
  val Client2 = system.actorOf(Props(new ClientActor(RandomValues.clients(1), Apartment, LocalDate.of(2022,7,29), ListBuffer(), searchActor)), "searchApartment")
  Thread.sleep(1000)
  val Client3 = system.actorOf(Props(new ClientActor(RandomValues.clients(2), Resort, LocalDate.of(2022,7,26), ListBuffer(), searchActor)), "searchResort")
  Thread.sleep(1000)
  val Client4 = system.actorOf(Props(new ClientActor(RandomValues.clients(3), Hotel, LocalDate.of(2022,7,27), ListBuffer(), searchActor)), "searchHotel2")
  Thread.sleep(1000)
  val Client5 = system.actorOf(Props(new ClientActor(RandomValues.clients(4), Apartment, LocalDate.of(2022,7,30), ListBuffer(), searchActor)), "searchApartment2")
  Thread.sleep(1000)
  val Client6 = system.actorOf(Props(new ClientActor(RandomValues.clients(5), Resort, LocalDate.of(2022,7,31), ListBuffer(), searchActor)), "searchResort2")
  Thread.sleep(1000)
  val Client7 = system.actorOf(Props(new ClientActor(RandomValues.clients(6), Hotel, LocalDate.of(2022,7,25), ListBuffer(), searchActor)), "searchHotel3")
  Thread.sleep(1000)
  val Client8 = system.actorOf(Props(new ClientActor(RandomValues.clients(7), Apartment, LocalDate.of(2022,7,22), ListBuffer(), searchActor)), "searchApartment3")
  Thread.sleep(1000)
  val Client9 = system.actorOf(Props(new ClientActor(RandomValues.clients(8), Resort, LocalDate.of(2022,7,25), ListBuffer(), searchActor)), "searchResort3")
  Thread.sleep(1000)
  val Client10 = system.actorOf(Props(new ClientActor(RandomValues.clients(9), Hotel, LocalDate.of(2022,7,22), ListBuffer(), searchActor)), "searchHotel4")
  Thread.sleep(1000)

  /**
   * Uncomment these two to test for cancellation and reserving property upon cancellation
   */
//  val cancelReservation = system.actorOf(Props(new ClientActor(RandomValues.clients(0), None, LocalDate.now,
//    ListBuffer(Slug(true, UUID.fromString("fbd23f3f-3323-423f-8797-4d15c38f5cbf"))), searchActor)), "cancelReservation")
//  Thread.sleep(1000)
//  val reserveUponCancelled = system.actorOf(Props(new ClientActor(RandomValues.clients(6), None, LocalDate.of(2022,7,24),
//    ListBuffer(Slug(false, UUID.fromString("fbd23f3f-3323-423f-8797-4d15c38f5cbf"))), searchActor)), "reserveUponCancelled")

  Thread.sleep(5000)
  system.terminate()

}
