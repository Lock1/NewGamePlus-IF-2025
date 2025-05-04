{-# LANGUAGE LambdaCase #-}
import Data.Set
import Data.Map
import Data.List
import Data.Char
import Data.List.NonEmpty
import Prelude hiding ((^))
import qualified Prelude ((^))

-- Stopping -Wall from complaining all the times
(^) :: Int -> Int -> Int
(^) = (Prelude.^)


type BruteForceSolutionState = Map Char Int -- Contract: BruteForceSolution need to map all used char -> int
data Primitive = BoolExpression Bool | IntExpression Int deriving (Show, Eq)
data CryptarithmExpression =
    EvaluableExpression (BruteForceSolutionState -> Int)
    | PrimitiveExpression Primitive
    | EqualityExpression {lhs :: CryptarithmExpression, rhs :: CryptarithmExpression}
    | AdditionExpression {lhs :: CryptarithmExpression, rhs :: CryptarithmExpression}

instance Show CryptarithmExpression where
    show expr = case expr of
        EvaluableExpression _         -> "Evaluable"
        PrimitiveExpression p         -> show p
        EqualityExpression {lhs, rhs} -> "EqualityExpression:{" ++ show lhs ++ ", " ++ show rhs ++ "}"
        AdditionExpression {lhs, rhs} -> "AdditionExpression:{" ++ show lhs ++ ", " ++ show rhs ++ "}"



-- pointless :: Maybe Int -> Maybe Int -> Maybe Int
-- pointless = (<*>) . ((+) <$>) -- Pointless indeed: (+) <$> a <*> b
-- reducer :: [Maybe Int] -> Int -- C: Lesson learned. Fail-fast & explode or Maybe Int, DO NOT swallow. Nothing implies broken definition
reducer :: [Int] -> Int
reducer list = fst (Data.List.foldl' (\(acc, e) x -> (acc + x*10^e, e+1)) (0, 0) list)
sanityCheckTransform :: [Maybe Int] -> [Int]
sanityCheckTransform list = (\case Nothing -> error ("Explode" ++ show list); Just n -> n) <$> list
constructEvaluableExpression :: String -> CryptarithmExpression
constructEvaluableExpression str = let reverseLowerStr =  Data.List.reverse $ toLower <$> str in
    EvaluableExpression (\solution -> reducer . sanityCheckTransform $ Data.List.map (`Data.Map.lookup` solution) reverseLowerStr)



constructExpression :: Data.List.NonEmpty.NonEmpty String -> CryptarithmExpression -- Contract: At least 3 elements
constructExpression ls = let
    constantExprList = constructEvaluableExpression <$> ls
    in EqualityExpression {
        lhs=buildAdditionExpr $ Data.List.NonEmpty.init constantExprList,
        rhs=Data.List.NonEmpty.last constantExprList
    }
    where
        buildAdditionExpr :: [CryptarithmExpression] -> CryptarithmExpression
        buildAdditionExpr (expr1:expr2:addExprTail) = AdditionExpression {
            lhs=expr1,
            rhs=if Data.List.null addExprTail then expr2 else buildAdditionExpr (expr2:addExprTail)
        }
        buildAdditionExpr []  = error "Explode"
        buildAdditionExpr [_] = error "Explode"



evaluateToPrimitive :: CryptarithmExpression -> BruteForceSolutionState -> Primitive
evaluateToPrimitive expr solution = case expr of
    EvaluableExpression evalExpr  -> IntExpression . evalExpr $ solution
    PrimitiveExpression p         -> p
    EqualityExpression {lhs, rhs} -> BoolExpression $ evaluatePrimitive (==) lhs rhs
    AdditionExpression {lhs, rhs} -> IntExpression  $ evaluatePrimitive (+)  lhs rhs
    where
        evaluatePrimitive operator lhs rhs = case (evaluateToPrimitive lhs solution, evaluateToPrimitive rhs solution) of
            (IntExpression x, IntExpression y) -> x `operator` y
            (_, _)                             -> error "Explode"
evaluate :: CryptarithmExpression -> BruteForceSolutionState -> Bool
evaluate expr solution = case evaluateToPrimitive expr solution of
    BoolExpression b -> b
    IntExpression _  -> error "explode"




bruteForce :: Data.List.NonEmpty.NonEmpty String -> [BruteForceSolutionState]
bruteForce problem =
        Data.List.filter (evaluate cryptarithmExpr) solutionSpace
    where
        originalSet = Data.Set.fromList . Data.List.map toLower <$> problem
        uniqueCharList  = Data.Set.toList $ Data.List.foldl' Data.Set.union Data.Set.empty originalSet
        cryptarithmExpr = constructExpression problem
        solutionSpaceEnumerator (c:listTail) = [subTree `Data.Map.union` Data.Map.singleton c i | subTree <- solutionSpaceEnumerator listTail, i <- [0..9]]
        solutionSpaceEnumerator [] = [Data.Map.empty]
        solutionSpace = solutionSpaceEnumerator uniqueCharList




sampleProblem :: NonEmpty String
-- sampleProblem = Data.List.NonEmpty.fromList ["NO", "GUN", "NO", "HUNT"] -- "100" ./a  0.38s user 0.01s system 99% cpu 0.393 total
sampleProblem = Data.List.NonEmpty.fromList ["CROSS", "ROADS", "DANGER"]

main :: IO ()
main = print . show . Data.List.length . bruteForce $ sampleProblem