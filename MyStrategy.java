import model.*;

public final class MyStrategy implements Strategy {
    @Override
    
    public void act(Robot me, Rules rules, Game game, Action action) {
        Ball ball = game.ball;
        
        if (!me.touch)
        {
            action.target_velocity_x = 0.0;
            action.target_velocity_z = 0.0;
            action.target_velocity_y = -rules.MAX_ENTITY_SPEED;
            action.jump_speed = 0.0;
            action.use_nitro = true;
            return;
        }
        
        boolean jump = DistBetweenPoint(new Point(ball), new Point(me)) <
                (rules.BALL_RADIUS + rules.ROBOT_MAX_RADIUS) &&
                (ball.y - ball.radius) <= (rules.ROBOT_MAX_RADIUS*2.0) &&
                me.y < ball.y && 
                me.z < ball.z;
        
        boolean is_attacker = true;
        for (Robot robot : game.robots)
        {
            if (robot.is_teammate && robot.id != me.id)
            {
                if (robot.id > me.id)
                {
                    is_attacker = false;
                    break;
                }
            }
        }
        
        if ((is_attacker || ball.z < (-rules.arena.depth / 5)) && 
                ball.z > me.z)
        {
            for (int i = 0; i < 100; ++i)
            {
                double t = i * 0.1;
                Ball ball_pos = CalcNewBallPos(ball, t, rules);
                // Если мяч не вылетит за пределы арены
                // (произойдет столкновение со стеной, которое мы не рассматриваем),
                // и при этом мяч будет находится ближе к вражеским воротам, чем робот,
                if (ball_pos.z > me.z)
                    //&& Math.abs(ball.x) < (rules.arena.width / 2.0)
                    //&& Math.abs(ball.z) < (rules.arena.depth / 2.0))
                {
                    // Посчитаем, с какой скоростью робот должен бежать,
                    // Чтобы прийти туда же, где будет мяч, в то же самое время
                    double delta_pos_x = ball_pos.x - me.x;
                    double delta_pos_z = ball_pos.z - me.z;
                    double delta_pos_dist = Math.sqrt(delta_pos_x*delta_pos_x + delta_pos_z*delta_pos_z);
                    double need_speed = delta_pos_dist / t;
                    // Если эта скорость лежит в допустимом отрезке
                    if (0.5 * rules.ROBOT_MAX_GROUND_SPEED < need_speed
                        && need_speed < rules.ROBOT_MAX_GROUND_SPEED)
                    {
                        // То это и будет наше текущее действие
                        action.target_velocity_x = delta_pos_x / delta_pos_dist * need_speed;
                        action.target_velocity_z = delta_pos_z / delta_pos_dist * need_speed;
                        action.target_velocity_y = 0.0;
                        action.jump_speed = jump ? rules.ROBOT_MAX_JUMP_SPEED : 0.0;
                        action.use_nitro = false;
                        return;
                    }
                }
            }
        }
        // Стратегия защитника (или атакующего, не нашедшего хорошего момента для удара):
        // Будем стоять посередине наших ворот
        double target_pos_x = ball.x;
        if (target_pos_x > rules.arena.goal_width / 2)
        {
            target_pos_x = rules.arena.goal_width / 2;
        }
        
        if (target_pos_x < -rules.arena.goal_width / 2)
        {
            target_pos_x =  -rules.arena.goal_width / 2;
        }
        
        double target_pos_z = -(rules.arena.depth / 2.0) + rules.arena.bottom_radius - rules.ROBOT_MIN_RADIUS;
        // Причем, если мяч движется в сторону наших ворот
        if (ball.velocity_z < -EPS) {
            // Найдем время и место, в котором мяч пересечет линию ворот
            double t = (target_pos_z - ball.z) / ball.velocity_z;
            double x = ball.x + ball.velocity_x * t;
            // Если это место - внутри ворот
            if (Math.abs(x) < (rules.arena.goal_width / 2.0)) {
                // То пойдем защищать его
                target_pos_x = x;
            }
        }

        // Установка нужных полей для желаемого действия
        double target_velocity_x = (target_pos_x - me.x)*rules.ROBOT_MAX_GROUND_SPEED;
        double target_velocity_z = (target_pos_z - me.z)*rules.ROBOT_MAX_GROUND_SPEED;

        action.target_velocity_x = target_velocity_x;
        action.target_velocity_z = target_velocity_z;
        action.target_velocity_y = 0.0;
        action.jump_speed = jump ? rules.ROBOT_MAX_JUMP_SPEED : 0.0;
        action.use_nitro = false;
    }
    
    
    private static double DistBetweenPoint(Point p1, Point p2)
    {
        return Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.z - p2.z) * (p1.z - p2.z));
    }
    
    private static Ball CalcNewBallPos(Ball b, double t, Rules r)
    {
        Point newVel = Mult(new Point (b.velocity_x, b.velocity_y, b.velocity_z), t);        
        Point newPos = Add(newVel, new Point(b));
        
        Ball newBall = new Ball();
        
        newBall.x = newPos.x;
        newBall.y = newPos.y;
        newBall.z = newPos.z;
        newBall.radius = b.radius;
        newBall.velocity_x = newVel.x;
        newBall.velocity_y = newVel.y;
        newBall.velocity_z = newVel.z;
        return CollideWithArena(newBall, r);
    }
    
    private static double Dot(Point p1, Point p2)
    {
        return p1.x*p2.x + p1.y*p2.y + p1.z*p2.z;
    }
    
    private static Point Add(Point p1, Point p2)
    {
        return new Point (p1.x + p2.x, p1.y + p2.y, p1.z + p2.z);
    }
    
    private static Point Sub(Point p1, Point p2)
    {
        return new Point (p1.x - p2.x, p1.y - p2.y, p1.z - p2.z);
    }
    
    private static Point Mult(Point p1, double c)
    {
        return new Point (p1.x * c, p1.y * c, p1.z * c);
    }
    
    private static Dan DanToPlane(Point p, Point pOnPlane, Point normal)
    {
        Dan d = new Dan();
        d.normal = new Point(normal);
        d.dot = Dot(p, pOnPlane);
        return d;
    }
    
    private static Dan MinDan(Dan d1, Dan d2)
    {
        Dan min = d1;
        if (d1.dot > d2.dot)
        {
            min = d2;
        }
        return min;
    }
    
    private static Dan DanToArenaQuarter(Point p, Arena arena)
    {
        Dan dan = DanToPlane(p, new Point(0, 0, 0), new Point(0, 1, 0));
        dan = MinDan(dan, DanToPlane(p, new Point(0, arena.height, 0), new Point(0, -1, 0))); // celling
        dan = MinDan(dan, DanToPlane(p, new Point(arena.width/2, 0, 0), new Point(-1, 0, 0))); // x side
        dan = MinDan(dan, DanToPlane(p, new Point(-arena.width/2, 0, 0), new Point(1, 0, 0))); // x side 2
        dan = MinDan(dan, DanToPlane(p, new Point(0, 0, arena.depth / 2), new Point(0, 0, -1))); // z side
        dan = MinDan(dan, DanToPlane(p, new Point(0, 0, -arena.depth / 2), new Point(0, 0, 1))); // z side 2
        
        return dan;
    }
    
    private static Dan DanToArena(Point p, Arena arena)
    {
        boolean negate_x = p.x < 0;
        boolean negate_z = p.z < 0;
        if (negate_x)
        {
            p.x = -p.x;
        }
        
        if (negate_z)
        {
            p.z = -p.z;
        }
        Dan dan = DanToArenaQuarter(p, arena);
        if (negate_x)
        {
            dan.normal.x = -dan.normal.x;
        }
        if (negate_z)
        {
            dan.normal.z = -dan.normal.z;
        }
        return dan;
    }
    
    private static Ball CollideWithArena(Ball b, Rules r)
    {
        Point pos = new Point(b);
        Dan dan = DanToArena(pos, r.arena);
        
        
        double penetration = b.radius - dan.dot;
        if (penetration > 0)
        {
            pos = Add(pos, Mult(dan.normal, penetration));
            Point v = new Point(b.velocity_x, b.velocity_y, b.velocity_z);
            double velocity = Dot(v, dan.normal);
            if (velocity < 0)
            {
                v = Sub(v, Mult(dan.normal, (1 + r.BALL_ARENA_E)*velocity));

                Ball res = new Ball();
                res.velocity_x = v.x;
                res.velocity_y = v.y;
                res.velocity_z = v.z;

                res.x = pos.x;
                res.y = pos.y;
                res.z = pos.z;

                res.radius = b.radius;
                return res;
            }
        }     
        return b;
    }
    
    @Override
    public String customRendering() {
        return "";
    }
    private double EPS = 1e-5;
}

final class Point
{
    public Point(double sx, double sy, double sz)
    {
        x = sx;
        y = sy;
        z = sz;
    }
    public Point(Point p)
    {
        x = p.x;
        y = p.y;
        z = p.z;
    }
    public Point(Ball ball)
    {
        x = ball.x;
        y = ball.y;
        z = ball.z;
    }
    public Point(Robot r)
    {
        x = r.x;
        y = r.y;
        z = r.z;
    }
    public double x;
    public double y;
    public double z;

}

class Dan
{
    public Point normal;
    public double dot;
}