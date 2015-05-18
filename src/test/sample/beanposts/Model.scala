package sample.beanedposts

import java.time.LocalDateTime

import scala.beans.BeanProperty

// from http://stackoverflow.com/questions/23832136/scala-domain-object-modeling

sealed trait Id {
  @BeanProperty var strVal: String
}

case class UserId(strVal: String) extends Id

case class PostId(strVal: String) extends Id

trait Model {
  @BeanProperty var id: Id

  @BeanProperty var creationDate: java.time.LocalDateTime
}

case class User(
                 id: UserId,
                 creationDate: LocalDateTime,
                 name: String,
                 email: String
                 ) extends Model

trait Post extends Model {
  @BeanProperty var id: PostId

  @BeanProperty var user: User

  @BeanProperty var title: String

  @BeanProperty var body: String
}

trait Moderated {
  @BeanProperty var isApproved: Boolean
}

case class UnModeratedPost(
                            id: PostId,
                            creationDate: LocalDateTime,
                            user: User,
                            title: String,
                            body: String
                            ) extends Post

case class ModeratedPost(
                          id: PostId,
                          creationDate: LocalDateTime,
                          user: User,
                          title: String,
                          body: String,
                          isApproved: Boolean
                          ) extends Post with Moderated

