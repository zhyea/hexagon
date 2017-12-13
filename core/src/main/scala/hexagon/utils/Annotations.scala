package hexagon.utils

import scala.annotation.StaticAnnotation


/**
  * 表示被声明的类是线程安全的
  */
class threadSafe extends StaticAnnotation


/**
  * 表示被声明的类是非线程安全的
  */
class nonThreadSafe extends StaticAnnotation


/**
  * 表示被声明的类是不可变的
  */
class immutable extends StaticAnnotation



